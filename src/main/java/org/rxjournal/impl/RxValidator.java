package org.rxjournal.impl;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.ValueIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to help in validating data in rxJournal during testing.
 */
public class RxValidator {
    private static final Logger LOG = LoggerFactory.getLogger(RxValidator.class.getName());
    private ValidationResult validationResult;
    private DataItemProcessor dataItemProcessor = new DataItemProcessor();

    public Observable<ValidationResult> validate(String fileName, Observable observable, String filter) {
        Subject<ValidationResult> validatorPublisher = PublishSubject.create();
        ChronicleQueue queue = SingleChronicleQueueBuilder.binary(fileName).build();
        ExcerptTailer tailer = queue.createTailer();
        validationResult = new ValidationResult();

        observable.subscribe(generatedResult -> {
                    Object onQueue = getNextMatchingFilter(tailer, filter);
                    if (onQueue.equals(generatedResult)) {
                        validationResult.setResult(ValidationResult.Result.OK);
                    } else {
                        validationResult.setResult(ValidationResult.Result.BAD);
                    }
                    validationResult.setFromQueue(onQueue);
                    validationResult.setGenerated(generatedResult);
                    validatorPublisher.onNext(validationResult);
                },
                error -> {
                    LOG.error("error in validate [{}]", error);
                },
                () -> {
                    validatorPublisher.onComplete();
                    queue.close();
                });

        return validatorPublisher;
    }



    private Object getNextMatchingFilter(ExcerptTailer tailer, String filter){
        long index = tailer.index();
        DocumentContext dc = tailer.readingDocument();

        if(!dc.isPresent()){
            throw new IllegalStateException("No value left on queue");
        }

        ValueIn in = dc.wire().getValueIn();
        dataItemProcessor.process(in, null);

        if(dataItemProcessor.getFilter().equals(filter)){
            return dataItemProcessor.getObject();
        }else{
            tailer.moveToIndex(++index);
            return getNextMatchingFilter(tailer, filter);
        }

    }

    public ValidationResult getValidationResult(){
        return validationResult;
    }
}
