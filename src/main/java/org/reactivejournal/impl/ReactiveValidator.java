package org.reactivejournal.impl;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.wire.ValueIn;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to help in validating data in rxJournal during testing.
 */
public class ReactiveValidator {
    private static final Logger LOG = LoggerFactory.getLogger(ReactiveValidator.class.getName());
    private ValidationResult validationResult;
    private DataItemProcessor dataItemProcessor = new DataItemProcessor();

    public void validate(String fileName, Publisher flowable, String filter, Subscriber subscriber) {
        ChronicleQueue queue = SingleChronicleQueueBuilder.binary(fileName).build();
        ExcerptTailer tailer = queue.createTailer();
        validationResult = new ValidationResult();

        flowable.subscribe(new Subscriber() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Object o) {
                Object onQueue = null;
                try {
                    onQueue = getNextMatchingFilter(tailer, filter);
                }catch(IllegalStateException e){
                    subscriber.onComplete();
                }catch(Exception e){
                    subscriber.onError(e);
                }

                if (onQueue.equals(o)) {
                    validationResult.setResult(ValidationResult.Result.OK);
                } else {
                    validationResult.setResult(ValidationResult.Result.BAD);
                }
                validationResult.setFromQueue(onQueue);
                validationResult.setGenerated(o);
                subscriber.onNext(validationResult);
            }

            @Override
            public void onError(Throwable throwable) {
                LOG.error("error in validate [{}]", throwable);
            }

            @Override
            public void onComplete() {
                subscriber.onComplete();
                queue.close();
            }
        });
    }


    private Object getNextMatchingFilter(ExcerptTailer tailer, String filter) {
        nextItemFromQueue(tailer, dataItemProcessor);
        System.out.println(dataItemProcessor);
        if (dataItemProcessor.getFilter().equals(filter)) {
            return dataItemProcessor.getObject();
        } else {
            return getNextMatchingFilter(tailer, filter);
        }

    }

    private boolean nextItemFromQueue(ExcerptTailer tailer,DataItemProcessor dim) {
        return !tailer.readDocument(w -> {
            ValueIn in = w.getValueIn();
            dim.process(in, null);
        });
    }

    public ValidationResult getValidationResult() {
        return validationResult;
    }
}
