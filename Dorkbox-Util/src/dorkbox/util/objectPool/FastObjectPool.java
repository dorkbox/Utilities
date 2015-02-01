/*
 * from: http://ashkrit.blogspot.de/2013/05/lock-less-java-object-pool.html
 *       https://github.com/ashkrit/blog/tree/master/FastObjectPool
 * copyright ashkrit 2013
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modified by dorkbox, llc
 */
package dorkbox.util.objectPool;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.locks.ReentrantLock;


class FastObjectPool<T> implements ObjectPool<T> {

    private final sun.misc.Unsafe unsafe;

    private static final boolean FREE = true;
    private static final boolean USED = false;

    private final ObjectPoolHolder<T>[] objects;

    private volatile int takePointer;
    private volatile int releasePointer;

    private final int mask;
    private final long BASE;
    private final long INDEXSCALE;
    private final long ASHIFT;

    public ReentrantLock lock = new ReentrantLock();
    private ThreadLocal<ObjectPoolHolder<T>> localValue = new ThreadLocal<>();

    FastObjectPool(PoolableObject<T> poolableObject, int size) {
        try {
            final PrivilegedExceptionAction<sun.misc.Unsafe> action = new PrivilegedExceptionAction<sun.misc.Unsafe>() {
                @Override
                public sun.misc.Unsafe run() throws Exception {
                    Class<sun.misc.Unsafe> unsafeClass = sun.misc.Unsafe.class;
                    Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
                    theUnsafe.setAccessible(true);
                    Object unsafeObject = theUnsafe.get(null);
                    if (unsafeClass.isInstance(unsafeObject)) {
                        return unsafeClass.cast(unsafeObject);
                    }

                    throw new NoSuchFieldError("the Unsafe");
                }
            };

            this.unsafe = AccessController.doPrivileged(action);
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to load unsafe", e);
        }


        int newSize = 1;
        while (newSize < size) {
            newSize = newSize << 1;
        }

        size = newSize;

        @SuppressWarnings({"unchecked", "rawtypes"})
        ObjectPoolHolder<T>[] stuff = new ObjectPoolHolder[size];
        this.objects = stuff;

        for (int x=0;x<size;x++) {
            this.objects[x] = new ObjectPoolHolder<T>(poolableObject.create());
        }

        this.mask = size-1;
        this.releasePointer = size;
        this.BASE = this.unsafe.arrayBaseOffset(ObjectPoolHolder[].class);
        this.INDEXSCALE = this.unsafe.arrayIndexScale(ObjectPoolHolder[].class);
        this.ASHIFT = 31 - Integer.numberOfLeadingZeros((int) this.INDEXSCALE);
    }

    @Override
    public ObjectPoolHolder<T> take() {
        int localTakePointer;

        // if we have an object available in the cache, use it instead.
        ObjectPoolHolder<T> localObject = this.localValue.get();
        if (localObject != null) {
            if (localObject.state.compareAndSet(FREE, USED)) {
                return localObject;
            }
        }

        sun.misc.Unsafe unsafe = this.unsafe;

        while (this.releasePointer != (localTakePointer=this.takePointer)) {
            int index = localTakePointer & this.mask;

            ObjectPoolHolder<T> holder = this.objects[index];
            //if(holder!=null && THE_UNSAFE.compareAndSwapObject(objects, (index*INDEXSCALE)+BASE, holder, null))
            if (holder != null && unsafe.compareAndSwapObject(this.objects, (index<<this.ASHIFT)+this.BASE, holder, null)) {
                this.takePointer = localTakePointer+1;

                // the use of a threadlocal reference here helps eliminates contention. This also checks OTHER threads,
                // as they might have one sitting on the cache
                if (holder.state.compareAndSet(FREE, USED)) {
                    this.localValue.set(holder);
                    return holder;
                }
            }
        }
        return null;
    }

    @Override
    public void release(ObjectPoolHolder<T> object) {
        try {
            this.lock.lockInterruptibly();

            int localValue = this.releasePointer;
            //long index = ((localValue & mask) * INDEXSCALE ) + BASE;
            long index = ((localValue & this.mask)<<this.ASHIFT ) + this.BASE;

            if (object.state.compareAndSet(USED, FREE)) {
                this.unsafe.putOrderedObject(this.objects, index, object);
                this.releasePointer = localValue+1;
            }
            else {
                throw new IllegalArgumentException("Invalid reference passed");
            }
        } catch (InterruptedException e) {
        }
        finally {
            this.lock.unlock();
        }
    }
}