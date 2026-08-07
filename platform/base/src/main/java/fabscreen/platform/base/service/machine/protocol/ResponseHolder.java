package fabscreen.platform.base.service.machine.protocol;

import fabscreen.platform.base.service.machine.IStructure;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

public class ResponseHolder {
    private BehaviorSubject<IStructure> responseSubject;
    private IStructure responseStructure;
    private Disposable writerDisposable;

    public ResponseHolder(BehaviorSubject<IStructure> subject, IStructure structure, Disposable writerDisposable) {
        this.responseSubject = subject;
        this.responseStructure = structure;
        this.writerDisposable = writerDisposable;
    }

    public BehaviorSubject<IStructure> getResponseSubject() {
        return responseSubject;
    }

    public void setResponseSubject(BehaviorSubject<IStructure> responseSubject) {
        this.responseSubject = responseSubject;
    }

    public IStructure getResponseStructure() {
        return responseStructure;
    }

    public void setResponseStructure(IStructure responseStructure) {
        this.responseStructure = responseStructure;
    }

    public void close() {
        if (responseSubject != null) {
            responseSubject.onComplete();
        }
        if (writerDisposable != null && !writerDisposable.isDisposed()) {
            writerDisposable.dispose();
        }
    }
}
