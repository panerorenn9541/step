// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import java.lang.Math;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public final class FindMeetingQuery {
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    if (request.getAttendees().isEmpty()) {
      return Arrays.asList(TimeRange.WHOLE_DAY);
    }
    if (request.getDuration() > TimeRange.WHOLE_DAY.duration()) {
      return Arrays.asList();
    }

    Map<String, ArrayList<TimeRange>> schedules = new HashMap<String, ArrayList<TimeRange>>();
    for (String requestAttendee : request.getAttendees()) {
      schedules.put(requestAttendee, new ArrayList(Arrays.asList(TimeRange.WHOLE_DAY)));
    }

    for (Event event : events) {
      for (String attendee : event.getAttendees()) {
        if (schedules.containsKey(attendee)) {
          for (ListIterator scheduleIterator = schedules.get(attendee).listIterator();
            scheduleIterator.hasNext();) {
            TimeRange freeTime = (TimeRange) scheduleIterator.next();
            if (freeTime.equals(event.getWhen())) {
              scheduleIterator.remove();
            } else if (freeTime.contains(event.getWhen())) {
              scheduleIterator.remove();
              if (freeTime.start() != event.getWhen().start()) {
                scheduleIterator.add(
                  TimeRange.fromStartEnd(freeTime.start(), event.getWhen().start(), false));
              }
              if (freeTime.end() != event.getWhen().end()) {
                scheduleIterator.add(
                  TimeRange.fromStartEnd(event.getWhen().end(), freeTime.end(), false));
              }
            } else if (freeTime.overlaps(event.getWhen())) {
              scheduleIterator.remove();
              if (freeTime.start() < event.getWhen().end()) {
                scheduleIterator.add(
                  TimeRange.fromStartEnd(event.getWhen().end(), freeTime.end(), false));
              } else if (freeTime.end() > event.getWhen().start()) {
                scheduleIterator.add(
                  TimeRange.fromStartEnd(freeTime.start(), event.getWhen().start(), false));
              }
            }
          }
        }
      }
    }

    int firstFlag = 1;
    ArrayList<TimeRange> meetingTimes = new ArrayList<TimeRange>();
    ArrayList<TimeRange> meetingTimesToAdd = new ArrayList<TimeRange>();
    for (String attendee : schedules.keySet()) {
      if (firstFlag == 1) {
        firstFlag = 0;
        for (TimeRange free : schedules.get(attendee)) {
          if (free.duration() >= request.getDuration())
            meetingTimes.add(free);
        }
      } else {
        for (ListIterator meetingTimesIterator = meetingTimes.listIterator();
          meetingTimesIterator.hasNext();) {
          TimeRange firstFree = (TimeRange) meetingTimesIterator.next();
          for (TimeRange nextFree : schedules.get(attendee)) {
            if (nextFree.overlaps(firstFree)) {
              meetingTimesToAdd.add(
                TimeRange.fromStartEnd(Math.max(firstFree.start(), nextFree.start()),
                  Math.min(firstFree.end(), nextFree.end()), false));
            }
          }
          meetingTimesIterator.remove();
        }
      }
      for (TimeRange adding : meetingTimesToAdd) {
        meetingTimes.add(adding);
      }
    }
    return meetingTimes;
  }
}
