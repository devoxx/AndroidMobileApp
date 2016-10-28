package com.devoxx.connection.model;

import com.annimon.stream.function.Predicate;
import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;

import java.io.Serializable;

public class SlotApiModel implements Serializable, Comparable<SlotApiModel> {

    /* nullability is mutually exclusive with talk field */
    @SerializedName("break")
    public BreakApiModel slotBreak;

    /* nullability is mutually exclusive with talk break */
    public TalkFullApiModel talk;

    public String roomId;
    public String roomSetup;
    public String toTime;
    public String fromTime;
    public String roomName;
    public String slotId;
    public String day;
    public boolean notAllocated;
    long fromTimeMillis;
    long toTimeMillis;
    public int roomCapacity;

    public DateTime fromTime() {
        return new DateTime(fromTimeMillis);
    }

    public DateTime toTime() {
        return new DateTime(toTimeMillis);
    }

    public long fromTimeMs() {
        return fromTimeMillis;
    }

    public long toTimeMs() {
        return toTimeMillis;
    }

    public boolean isBreak() {
        return slotBreak != null && talk == null;
    }

    public boolean isTalk() {
        return slotBreak == null && talk != null;
    }


    public static class SameModelPredicate implements Predicate<SlotApiModel> {

        private String id;

        public SameModelPredicate(String id) {
            this.id = id;
        }

        @Override
        public boolean test(SlotApiModel value) {
            return value.slotId.equals(id);
        }
    }

    public static class FilterPredicate implements Predicate<SlotApiModel> {

        private String query;

        public FilterPredicate() {

        }

        public FilterPredicate(String query) {
            this.query = query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        @Override
        public boolean test(SlotApiModel value) {
            return value.isTalk() && (value.talk.track.toLowerCase().contains(query)
                    || value.talk.title.toLowerCase().contains(query)
                    || value.talk.getReadableSpeakers().toLowerCase().contains(query)
                    || value.talk.summary.toLowerCase().contains(query));
        }
    }

    @Override
    public int compareTo(SlotApiModel another) {
        if (this.fromTimeMs() > another.fromTimeMs()) {
            return 1;
        } else if (this.fromTimeMs() < another.fromTimeMs()) {
            return -1;
        } else {
            return 0;
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SlotApiModel that = (SlotApiModel) o;

        if (roomId != null ? !roomId.equals(that.roomId) : that.roomId != null) return false;
        return slotId != null ? slotId.equals(that.slotId) : that.slotId == null;

    }

    @Override
    public int hashCode() {
        int result = roomId != null ? roomId.hashCode() : 0;
        result = 31 * result + (slotId != null ? slotId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SlotApiModel{" +
                "slotBreak=" + slotBreak +
                ", talk=" + talk +
                ", roomId='" + roomId + '\'' +
                ", roomSetup='" + roomSetup + '\'' +
                ", toTime='" + toTime + '\'' +
                ", fromTime='" + fromTime + '\'' +
                ", roomName='" + roomName + '\'' +
                ", slotId='" + slotId + '\'' +
                ", day='" + day + '\'' +
                ", notAllocated=" + notAllocated +
                ", fromTimeMillis=" + fromTimeMillis +
                ", toTimeMillis=" + toTimeMillis +
                ", roomCapacity=" + roomCapacity +
                '}';
    }
}
