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
    ArrayList<TimeRange> meetingTimes = new ArrayList<TimeRange>();
    // If there are no mandatory attendees, the whole day is free for optional
    if (request.getAttendees().isEmpty()) {
      meetingTimes.add(TimeRange.WHOLE_DAY);
    }
    // If the meeting duration is longer than a day, there is no slot available
    if (request.getDuration() > TimeRange.WHOLE_DAY.duration()) {
      return Arrays.asList();
    }

    Map<String, ArrayList<TimeRange>> schedules = new HashMap<String, ArrayList<TimeRange>>();
    schedules = initSchedules(request, schedules);

    // Update each attendee's schedule based on events
    for (Event event : events) {
      for (String attendee : event.getAttendees()) {
        if (schedules.containsKey(attendee)) {
          for (ListIterator scheduleIterator = schedules.get(attendee).listIterator();
               scheduleIterator.hasNext();) {
            schedules = scheduleEvent(event, attendee, scheduleIterator, schedules);
          }
        }
      }
    }

    // Takes into consideration all attendees to find free slots
    int firstFlag = 1;
    ArrayList<TimeRange> meetingTimesToAdd = new ArrayList<TimeRange>();
    for (String attendee : request.getAttendees()) {
      meetingTimesToAdd = determineAvailableTime(
          request, firstFlag, meetingTimesToAdd, schedules, attendee, meetingTimes);
      for (TimeRange adding : meetingTimesToAdd) {
        meetingTimes.add(adding);
      }
      meetingTimesToAdd.clear();
      firstFlag = 0;
    }

    // Takes into consideration all optional attenddes to find free slots
    ArrayList<TimeRange> optionalMeetingTimesToAdd = new ArrayList<TimeRange>();
    Map<String, ArrayList<TimeRange>> optionalFrees = new HashMap<String, ArrayList<TimeRange>>();
    for (String optionalAttendee : request.getOptionalAttendees()) {
      firstFlag = 0;
      ArrayList<TimeRange> optionalMeetingTimes = (ArrayList<TimeRange>) meetingTimes.clone();
      optionalMeetingTimesToAdd = determineAvailableTime(request, firstFlag,
          optionalMeetingTimesToAdd, schedules, optionalAttendee, optionalMeetingTimes);
      ArrayList<TimeRange> toPut = (ArrayList<TimeRange>) optionalMeetingTimesToAdd.clone();
      if (!toPut.isEmpty()) {
        optionalFrees.put(optionalAttendee, toPut);
      }
      optionalMeetingTimesToAdd.clear();
    }

    // If only one optional can attend, return that slot
    if (optionalFrees.size() == 1) {
      return optionalFrees.values().iterator().next();
    }

    // Find slots where the most optionals can attend
    Map<TimeRange, Integer> freeCounter = new HashMap<TimeRange, Integer>();
    freeCounter = determineFrequency(freeCounter, optionalFrees, request);
    int maximum = 0;
    ArrayList<TimeRange> finalTimes = new ArrayList<TimeRange>();
    for (TimeRange window : freeCounter.keySet()) {
      if (freeCounter.get(window) > maximum) {
        maximum = freeCounter.get(window);
      }
    }

    // If no free slots are available, return the mandatory free slots
    if (maximum == 0) {
      return meetingTimes;
    }

    // Otherwise, find the most available windows
    for (TimeRange window : freeCounter.keySet()) {
      if (freeCounter.get(window) == maximum) {
        finalTimes.add(window);
      }
    }
    Collections.sort(finalTimes, TimeRange.ORDER_BY_START);
    return finalTimes;
  }

  private Map<String, ArrayList<TimeRange>> initSchedules(
      MeetingRequest request, Map<String, ArrayList<TimeRange>> schedules) {
    // Initialize the schedules map, making each attendee completely free
    for (String requestAttendee : request.getAttendees()) {
      schedules.put(requestAttendee, new ArrayList(Arrays.asList(TimeRange.WHOLE_DAY)));
    }
    for (String optionalAttendee : request.getOptionalAttendees()) {
      schedules.put(optionalAttendee, new ArrayList(Arrays.asList(TimeRange.WHOLE_DAY)));
    }
    return schedules;
  }

  private Map<String, ArrayList<TimeRange>> scheduleEvent(Event event, String attendee,
      ListIterator scheduleIterator, Map<String, ArrayList<TimeRange>> schedules) {
    // Determine an attendee's updated availability based on an event
    TimeRange freeTime = (TimeRange) scheduleIterator.next();
    // If the event takes the whole slot, remove the slot
    if (freeTime.equals(event.getWhen())) {
      scheduleIterator.remove();
      // If the event is contained within the free slot, shorten the slot
    } else if (freeTime.contains(event.getWhen())) {
      scheduleIterator.remove();
      if (freeTime.start() != event.getWhen().start()) {
        scheduleIterator.add(
            TimeRange.fromStartEnd(freeTime.start(), event.getWhen().start(), false));
      }
      if (freeTime.end() != event.getWhen().end()) {
        scheduleIterator.add(TimeRange.fromStartEnd(event.getWhen().end(), freeTime.end(), false));
      }
      // If the event overlaps with the free slot, shorten the slot based on whether the start or
      // end overlaps.
    } else if (freeTime.overlaps(event.getWhen())) {
      scheduleIterator.remove();
      if (freeTime.start() < event.getWhen().end()) {
        scheduleIterator.add(TimeRange.fromStartEnd(event.getWhen().end(), freeTime.end(), false));
      } else if (freeTime.end() > event.getWhen().start()) {
        scheduleIterator.add(
            TimeRange.fromStartEnd(freeTime.start(), event.getWhen().start(), false));
      }
    }
    return schedules;
  }

  private ArrayList<TimeRange> determineAvailableTime(MeetingRequest request, int firstFlag,
      ArrayList<TimeRange> meetingTimesToAdd, Map<String, ArrayList<TimeRange>> schedules,
      String attendee, ArrayList<TimeRange> meetingTimes) {
    // Finds slot(s) where attendee is available in relation to currently available meetingTimes.
    if (firstFlag == 1) {
      // If first attendee, simply insert all free slots.
      for (TimeRange free : schedules.get(attendee)) {
        if (free.duration() >= request.getDuration()) {
          meetingTimesToAdd.add(free);
        }
      }
      // If subsequent attendee, find overlapping time slots and insert the most conservative time
      // slot.
    } else {
      for (ListIterator meetingTimesIterator = meetingTimes.listIterator();
           meetingTimesIterator.hasNext();) {
        TimeRange firstFree = (TimeRange) meetingTimesIterator.next();
        for (TimeRange nextFree : schedules.get(attendee)) {
          if (nextFree.overlaps(firstFree)) {
            TimeRange toAdd = TimeRange.fromStartEnd(Math.max(firstFree.start(), nextFree.start()),
                Math.min(firstFree.end(), nextFree.end()), false);
            if (toAdd.duration() >= request.getDuration()) {
              meetingTimesToAdd.add(toAdd);
            }
          }
        }
        meetingTimesIterator.remove();
      }
    }
    return meetingTimesToAdd;
  }

  private Map<TimeRange, Integer> determineFrequency(Map<TimeRange, Integer> freeCounter,
      Map<String, ArrayList<TimeRange>> optionalFrees, MeetingRequest request) {
    // Find how many optionals can attend each free slot
    for (String first : optionalFrees.keySet()) {
      for (String second : optionalFrees.keySet()) {
        if (first != second) {
          for (TimeRange firstTime : optionalFrees.get(first)) {
            for (TimeRange secondTime : optionalFrees.get(second)) {
              if (firstTime.overlaps(secondTime)) {
                TimeRange common =
                    TimeRange.fromStartEnd(Math.max(firstTime.start(), secondTime.start()),
                        Math.min(firstTime.end(), secondTime.end()), false);
                if (common.duration() >= request.getDuration()) {
                  // If slot has already been seen, increment by one
                  if (freeCounter.containsKey(common)) {
                    freeCounter.put(common, freeCounter.get(common) + 1);
                  } else {
                    int containFlag = 0;
                    for (TimeRange already : freeCounter.keySet()) {
                      // If slot is a subset or superset, combine the slot frequencies
                      if (already.contains(common)) {
                        freeCounter.put(common, freeCounter.get(already) + 2);
                        containFlag = 1;
                      } else if (common.contains(already)) {
                        freeCounter.put(already, freeCounter.get(already) + 2);
                        containFlag = 1;
                      }
                    }
                    // If slot is new, start keeping track of it
                    if (containFlag == 0) {
                      freeCounter.put(common, 1);
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    return freeCounter;
  }
}
