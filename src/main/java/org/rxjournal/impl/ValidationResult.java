package org.rxjournal.impl;

/**
 * Created by daniel on 19/04/17.
 */
public class ValidationResult<T> {
    public enum Result {OK, BAD}

    private Result result;
    private T fromQueue, generated;
    private Result summaryResult = null;
    private int totalCount, totalFailures, totalCorrect;

    public Result getResult() {
        return result;
    }

    void setResult(Result result) {
        totalCount++;
        this.result = result;
        if(result == Result.BAD){
            summaryResult = Result.BAD;
            totalFailures++;
        }else{
            if(summaryResult==null){
                summaryResult = Result.OK;
            }
            totalCorrect++;
        }
    }

    public T getItemFromQueue() {
        return fromQueue;
    }

    public void setFromQueue(T fromQueue) {
        this.fromQueue = fromQueue;
    }

    public T getItemObservable() {
        return generated;
    }

    public void setGenerated(T generated) {
        this.generated = generated;
    }

    public Result summaryResult(){
        return summaryResult;
    }

    public int summaryItemsCompared(){
        return totalCount;
    }

    public int summaryItemsValid(){
        return totalCorrect;
    }

    public int summmaryItemsFailed(){
        return totalFailures;
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "result=" + result +
                ", fromQueue=" + fromQueue +
                ", generated=" + generated +
                ", summaryResult=" + summaryResult +
                ", totalCount=" + totalCount +
                ", totalFailures=" + totalFailures +
                ", totalCorrect=" + totalCorrect +
                '}';
    }
}
