package dorkbox.util.objectPool;

/*
 *
 * from: http://ashkrit.blogspot.de/2013/05/lock-less-java-object-pool.html
 *       https://github.com/ashkrit/blog/tree/master/FastObjectPool
 * copyright ashkrit 2013
 *
 * MODIFIED by Nathan Robinson to be a version that is compatible with Android.
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

import java.util.concurrent.LinkedBlockingDeque;


class SlowObjectPool<T> implements ObjectPool<T> {

    private static final boolean FREE = true;
    private static final boolean USED = false;


    private final LinkedBlockingDeque<ObjectPoolHolder<T>> queue;
    private final PoolableObject<T> poolableObject;

    private ThreadLocal<ObjectPoolHolder<T>> localValue = new ThreadLocal<>();

    SlowObjectPool(PoolableObject<T> poolableObject, int size) {

        this.queue = new LinkedBlockingDeque<ObjectPoolHolder<T>>(size);

        this.poolableObject = poolableObject;
        for (int x=0;x<size;x++) {
            this.queue.add(new ObjectPoolHolder<T>(poolableObject.create()));
        }
    }

    @Override
    public ObjectPoolHolder<T> take() {
        // if we have an object available in the cache, use it instead.
        ObjectPoolHolder<T> localObject = this.localValue.get();
        if (localObject != null) {
            if (localObject.state.compareAndSet(FREE, USED)) {
                this.poolableObject.activate(localObject.getValue());
                return localObject;
            }
        }

        ObjectPoolHolder<T> holder = this.queue.poll();

        if (holder == null) {
            return null;
        }

        // the use of a threadlocal reference here helps eliminates contention. This also checks OTHER threads,
        // as they might have one sitting on the cache
        if (holder.state.compareAndSet(FREE, USED)) {
            this.localValue.set(holder);
            this.poolableObject.activate(holder.getValue());
            return holder;
        } else {
            // put it back into the queue
            this.queue.offer(holder);
            return null;
        }
    }

    @Override
    public void release(ObjectPoolHolder<T> object) {
        if (object.state.compareAndSet(USED, FREE)) {
            this.queue.offer(object);
            this.poolableObject.passivate(object.getValue());
        }
        else {
            throw new IllegalArgumentException("Invalid reference passed");
        }
    }
}