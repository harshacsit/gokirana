package com.harsha.kirana;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A LiveData that fires its value exactly ONCE.
 * After the observer consumes the value, it is cleared.
 * New observers registering later get nothing — no stale re-delivery.
 *
 * This completely solves the "notification on every screen switch" problem.
 */
public class SingleLiveEvent<T> extends MutableLiveData<T> {

    private final AtomicBoolean pending = new AtomicBoolean(false);

    @Override
    public void observe(LifecycleOwner owner, Observer<? super T> observer) {
        super.observe(owner, value -> {
            // Only deliver if there is a genuinely new value pending
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(value);
            }
            // Otherwise silently ignore — this is a re-subscription, not a new event
        });
    }

    @Override
    public void setValue(T value) {
        pending.set(true);   // mark as pending BEFORE setting value
        super.setValue(value);
    }

    @Override
    public void postValue(T value) {
        pending.set(true);
        super.postValue(value);
    }
}