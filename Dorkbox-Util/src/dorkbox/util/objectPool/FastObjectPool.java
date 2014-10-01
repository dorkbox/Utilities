package dorkbox.util.objectPool;

/*
 *
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
 */

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.locks.ReentrantLock;

import sun.misc.Unsafe;


public class FastObjectPool<T> {
    public static final Unsafe THE_UNSAFE;
    static {
        try {
            final PrivilegedExceptionAction<Unsafe> action = new PrivilegedExceptionAction<Unsafe>() {
                @Override
                public Unsafe run() throws Exception {
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

            THE_UNSAFE = AccessController.doPrivileged(action);
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to load unsafe", e);
        }
    }

    private Holder<T>[] objects;

    private volatile int takePointer;
    private int releasePointer;

    private final int mask;
    private final long BASE;
    private final long INDEXSCALE;
    private final long ASHIFT;

    public ReentrantLock lock = new ReentrantLock();
    private ThreadLocal<Holder<T>> localValue = new ThreadLocal<>();

    public FastObjectPool(PoolFactory<T> factory, int size) {

        int newSize = 1;
        while (newSize < size) {
            newSize = newSize << 1;
        }

        size = newSize;

        @SuppressWarnings({"unchecked", "rawtypes"})
        Holder<T>[] stuff = new Holder[size];
        this.objects = stuff;

        for (int x=0;x<size;x++) {
            this.objects[x] = new Holder<T>(factory.create());
        }

        this.mask = size-1;
        this.releasePointer = size;
        this.BASE = THE_UNSAFE.arrayBaseOffset(Holder[].class);
        this.INDEXSCALE = THE_UNSAFE.arrayIndexScale(Holder[].class);
        this.ASHIFT = 31 - Integer.numberOfLeadingZeros((int) this.INDEXSCALE);
    }

    public Holder<T> take() {
        int localTakePointer;

        Holder<T> localObject = this.localValue.get();
        if (localObject != null) {
            if(localObject.state.compareAndSet(Holder.FREE, Holder.USED)) {
                return localObject;
            }
        }

        while (this.releasePointer != (localTakePointer=this.takePointer)) {
            int index = localTakePointer & this.mask;
            Holder<T> holder = this.objects[index];
            //if(holder!=null && THE_UNSAFE.compareAndSwapObject(objects, (index*INDEXSCALE)+BASE, holder, null))
            if (holder != null && THE_UNSAFE.compareAndSwapObject(this.objects, (index<<this.ASHIFT)+this.BASE, holder, null)) {
                this.takePointer = localTakePointer+1;
                if (holder.state.compareAndSet(Holder.FREE, Holder.USED)) {
                    this.localValue.set(holder);
                    return holder;
                }
            }
        }
        return null;
    }

    public void release(Holder<T> object) throws InterruptedException {
        this.lock.lockInterruptibly();

        try {
            int localValue=this.releasePointer;
            //long index = ((localValue & mask) * INDEXSCALE ) + BASE;
            long index = ((localValue & this.mask)<<this.ASHIFT ) + this.BASE;

            if (object.state.compareAndSet(Holder.USED, Holder.FREE)) {
                THE_UNSAFE.putOrderedObject(this.objects, index, object);
                this.releasePointer = localValue+1;
            }
            else {
                throw new IllegalArgumentException("Invalid reference passed");
            }
        }
        finally {
            this.lock.unlock();
        }
    }
}