package com.vdotok.icsdks.cancelable;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.Disposable;

/**
 * Created By: VdoTok
 * Date & Time: On 2/16/21 At 12:14 PM in 2021
 *
 * Class implementing Cancelable interface to add remove cancelable objects
 */
public class Cancellables implements Cancelable {

    private List<Cancelable> cancelables = new ArrayList<>();

    public Cancellables() {

    }

    @Override
    public Boolean cancel() {
        for (Cancelable cancelable : cancelables) {
            cancelable.cancel();
        }
        cancelables.clear();
        return true;
    }

    public void add(Cancelable cancelable) {
        cancelables.add(cancelable);
    }

    void remove(Cancelable cancelable) {
        cancelables.remove(cancelable);
    }

    public static Cancelable of(Disposable disposable) {
        return new DisposableCancelable(disposable);
    }
}