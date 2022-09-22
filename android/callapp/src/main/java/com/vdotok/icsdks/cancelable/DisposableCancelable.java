package com.vdotok.icsdks.cancelable;

import io.reactivex.disposables.Disposable;

/**
 * Created By: VdoTok
 * Date & Time: On 2/16/21 At 12:14 PM in 2021
 *
 * Class implementing Cancelable to dispose of a cancelable object from memory
 */
public class DisposableCancelable implements Cancelable {

    private Disposable disposable = null;

    public DisposableCancelable(Disposable disposable) {
        this.disposable = disposable;
    }

    @Override
    public Boolean cancel() {

        boolean isAlreadyDisposed = disposable == null;
        if(disposable != null){
            disposable.dispose();
        }
        disposable = null;

        return isAlreadyDisposed;
    }
}
