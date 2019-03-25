package t.viewer.client.future;

import java.util.ArrayList;
import java.util.function.Consumer;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;

import t.common.shared.Pair;

/**
 * Represents an operation that will be completed in the future, either resulting 
 * in a value of some type or an exception.
 * 
 * @param <T> The type of result the operation will result in.
 */
public class Future<T> implements AsyncCallback<T> {
  private boolean done = false;
  private boolean fakeSuccess = false;
  private T result;
  private Throwable caught;
  private ArrayList<Consumer<Future<T>>> callbacks = new ArrayList<Consumer<Future<T>>>();
  
  public Future() {}
  
  public T result() {
    assert(done);
    return result;
  }
  
  public Throwable caught() {
    assert(done);
    return caught;
  }
  
  public boolean wasSuccessful() {
    assert(done);
    return caught == null;
  }
  
  public boolean done() {
    return done;
  }
  
  public boolean doneAndSuccessful() {
    return done() && wasSuccessful();
  }
  
  public boolean actuallyRan() {
    return done && !fakeSuccess;
  }
  
  public Future<T> addCallback(Consumer<Future<T>> callback) {
    if (!done) {
      callbacks.add(callback);
    } else {
      Scheduler.get().scheduleDeferred(() -> {
        callback.accept(this);
      });
    }
    return this;
  }
  
  public Future<T> addSuccessCallback(Consumer<T> callback) {
    addCallback(future -> {
      if (future.wasSuccessful()) {
        callback.accept(future.result());
      }
    });
    return this;
  }
  
  public void fakeSuccess(T t) {
    fakeSuccess = true;
    onSuccess(t);
  }
  
  @Override
  public void onSuccess(T t) {
    done = true;
    result = t;
    callbacks.forEach(c -> c.accept(this));
  }

  @Override
  public void onFailure(Throwable caught) {
    done = true;
    this.caught = caught;
    callbacks.forEach(c -> c.accept(this));
  }
  
  public static <T,U> Future<Pair<T,U>> combine(Future<T> future1, Future<U> future2) {
    Future<Pair<T,U>> combinedFuture = new Future<Pair<T,U>>();
    
    future1.addCallback(f -> {
      if (future2.done()) {
        combineResults(combinedFuture, future1, future2);
      }
    });
    
    future2.addCallback(f -> {
      if (future1.done()) {
        combineResults(combinedFuture, future1, future2);
      }
    });
    
    return combinedFuture;
  }
  
  /**
   * Precondition: both future1 and future2 done.
   */
  private static <T,U> void combineResults(Future<Pair<T,U>> 
      combinedFuture, Future<T> future1, Future<U> future2) {
    if (!future1.wasSuccessful()) {
      combinedFuture.onFailure(future1.caught());
    } else if (!future2.wasSuccessful()) {
      combinedFuture.onFailure(future2.caught());
    } else {
      combinedFuture.onSuccess(new Pair<T, U>(future1.result(), future2.result()));
    }
  }
}
