package org.rxrecorder.examples.fastproducerslowconsumer;

import net.openhft.chronicle.wire.Marshallable;

/**
 * Created by daniel on 06/12/16.
 */
public class MarketData implements Marshallable {
    private double bidPrice;
    private double askPrice;
    private int volume;
    private String id;

    public MarketData(){

    }

    public MarketData(String id, int volume, double bidPrice, double askPrice) {
        this.bidPrice = bidPrice;
        this.askPrice = askPrice;
        this.volume = volume;
        this.id = id;
    }

    public double getAskPrice() {
        return askPrice;
    }

    public void setAskPrice(double askPrice) {
        this.askPrice = askPrice;
    }

    public double getBidPrice() {
        return bidPrice;
    }

    public void setBidPrice(double bidPrice) {
        this.bidPrice = bidPrice;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "MarketData{" +
                "bidPrice=" + bidPrice +
                ", askPrice=" + askPrice +
                ", volume=" + volume +
                ", id='" + id + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MarketData that = (MarketData) o;

        if (Double.compare(that.bidPrice, bidPrice) != 0) return false;
        if (Double.compare(that.askPrice, askPrice) != 0) return false;
        if (volume != that.volume) return false;
        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(bidPrice);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(askPrice);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + volume;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }
}
