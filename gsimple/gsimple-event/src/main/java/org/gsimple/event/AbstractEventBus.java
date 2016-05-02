package org.gsimple.event;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.gsimple.common.utils.CheckObject;
import org.gsimple.event.annotation.DefaultEventListenerMethodAnnotation;
import org.gsimple.event.annotation.EventListenerMethod;

/**
 * Not thread safe
 * 
 * @author gaosong
 * 
 */
public abstract class AbstractEventBus implements EventBus {

	protected final EventListenerRegistryCenter eventListenerRegistryCenter;

	protected final ReadWriteLock lock = new ReentrantReadWriteLock();

	protected final Executor executor;

	protected final EventDispatcher eventDispatcher;

	public AbstractEventBus(Executor executor) {
		this(executor, EventListenerMethod.class,
				DefaultEventDispatcher.DIRECT_DISPATCHER);
	}

	public AbstractEventBus(Executor executor,
			Class<? extends Annotation> methodAnnotationClass) {
		this(executor, methodAnnotationClass,
				DefaultEventDispatcher.DIRECT_DISPATCHER);
	}

	public AbstractEventBus(Executor executor,
			Class<? extends Annotation> methodAnnotationClass,
			EventDispatcher eventDispatcher) {
		this.executor = executor;
		this.eventDispatcher = eventDispatcher;
		eventListenerRegistryCenter = new EventListenerRegistryCenter(
				new DefaultEventListenerMethodAnnotation(methodAnnotationClass));
	}

	@Override
	public void register(Object eventListener) {
		lock.writeLock().lock();
		try {
			eventListenerRegistryCenter.register(eventListener, executor);
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void unregister(Object eventListener) {
		lock.writeLock().lock();
		try {
			eventListenerRegistryCenter.unregister(eventListener);
		} finally {
			lock.writeLock().unlock();
		}

	}

	@Override
	public void publish(final Object event) {
		CheckObject.checkIsNull(event);
		Iterator<EventListener> eventListeners = eventListenerRegistryCenter
				.get(event);
		if (!CheckObject.isNull(eventListeners)) {
			eventDispatcher.dispatch(event, eventListeners);
		} else {
			if (!(event instanceof DeadEvent)) {
				publish(new DeadEvent(event));
			} else {
				// event ignored.
			}
		}
	}

}
