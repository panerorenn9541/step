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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** */
@RunWith(JUnit4.class)
public final class FindMeetingQueryTest {
  private static final Collection<Event> NO_EVENTS = Collections.emptySet();
  private static final Collection<String> NO_ATTENDEES = Collections.emptySet();

  // Some people that we can use in our tests.
  private static final String PERSON_A = "Person A";
  private static final String PERSON_B = "Person B";
  private static final String PERSON_C = "Person C";
  private static final String PERSON_D = "Person D";
  private static final String PERSON_E = "Person E";
  private static final String PERSON_F = "Person F";

  // All dates are the first day of the year 2020.
  private static final int TIME_0800AM = TimeRange.getTimeInMinutes(8, 0);
  private static final int TIME_0830AM = TimeRange.getTimeInMinutes(8, 30);
  private static final int TIME_0900AM = TimeRange.getTimeInMinutes(9, 0);
  private static final int TIME_0915AM = TimeRange.getTimeInMinutes(9, 15);
  private static final int TIME_0930AM = TimeRange.getTimeInMinutes(9, 30);
  private static final int TIME_0945AM = TimeRange.getTimeInMinutes(9, 45);
  private static final int TIME_1000AM = TimeRange.getTimeInMinutes(10, 0);
  private static final int TIME_1030AM = TimeRange.getTimeInMinutes(10, 30);
  private static final int TIME_1100AM = TimeRange.getTimeInMinutes(11, 00);

  private static final int DURATION_15_MINUTES = 15;
  private static final int DURATION_30_MINUTES = 30;
  private static final int DURATION_60_MINUTES = 60;
  private static final int DURATION_90_MINUTES = 90;
  private static final int DURATION_1_HOUR = 60;
  private static final int DURATION_2_HOUR = 120;

  private FindMeetingQuery query;

  @Before
  public void setUp() {
    query = new FindMeetingQuery();
  }

  @Test
  public void optionsForNoAttendees() {
    MeetingRequest request = new MeetingRequest(NO_ATTENDEES, DURATION_1_HOUR);

    Collection<TimeRange> actual = query.query(NO_EVENTS, request);
    Collection<TimeRange> expected = Arrays.asList(TimeRange.WHOLE_DAY);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void noOptionsForTooLongOfARequest() {
    // The duration should be longer than a day. This means there should be no options.
    int duration = TimeRange.WHOLE_DAY.duration() + 1;
    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), duration);

    Collection<TimeRange> actual = query.query(NO_EVENTS, request);
    Collection<TimeRange> expected = Arrays.asList();

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void eventSplitsRestriction() {
    // The event should split the day into two options (before and after the event).
    Collection<Event> events = Arrays.asList(new Event("Event 1",
        TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES), Arrays.asList(PERSON_A)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            TimeRange.fromStartEnd(TIME_0900AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void everyAttendeeIsConsidered() {
    // Have each person have different events. We should see two options because each person has
    // split the restricted times.
    //
    // Events  :       |--A--|     |--B--|
    // Day     : |-----------------------------|
    // Options : |--1--|     |--2--|     |--3--|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_B)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartEnd(TIME_0830AM, TIME_0900AM, false),
            TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void overlappingEvents() {
    // Have an event for each person, but have their events overlap. We should only see two options.
    //
    // Events  :       |--A--|
    //                     |--B--|
    // Day     : |---------------------|
    // Options : |--1--|         |--2--|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0830AM, DURATION_60_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_60_MINUTES),
            Arrays.asList(PERSON_B)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            TimeRange.fromStartEnd(TIME_1000AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void nestedEvents() {
    // Have an event for each person, but have one person's event fully contain another's event. We
    // should see two options.
    //
    // Events  :       |----A----|
    //                   |--B--|
    // Day     : |---------------------|
    // Options : |--1--|         |--2--|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0830AM, DURATION_90_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_B)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            TimeRange.fromStartEnd(TIME_1000AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void doubleBookedPeople() {
    // Have one person, but have them registered to attend two events at the same time.
    //
    // Events  :       |----A----|
    //                     |--A--|
    // Day     : |---------------------|
    // Options : |--1--|         |--2--|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0830AM, DURATION_60_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_A)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void justEnoughRoom() {
    // Have one person, but make it so that there is just enough room at one point in the day to
    // have the meeting.
    //
    // Events  : |--A--|     |----A----|
    // Day     : |---------------------|
    // Options :       |-----|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartEnd(TIME_0900AM, TimeRange.END_OF_DAY, true),
            Arrays.asList(PERSON_A)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void ignoresPeopleNotAttending() {
    // Add an event, but make the only attendee someone different from the person looking to book
    // a meeting. This event should not affect the booking.
    Collection<Event> events = Arrays.asList(new Event("Event 1",
        TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES), Arrays.asList(PERSON_A)));
    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_B), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected = Arrays.asList(TimeRange.WHOLE_DAY);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void noConflicts() {
    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(NO_EVENTS, request);
    Collection<TimeRange> expected = Arrays.asList(TimeRange.WHOLE_DAY);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void notEnoughRoom() {
    // Have one person, but make it so that there is not enough room at any point in the day to
    // have the meeting.
    //
    // Events  : |--A-----| |-----A----|
    // Day     : |---------------------|
    // Options :

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartEnd(TIME_0900AM, TimeRange.END_OF_DAY, true),
            Arrays.asList(PERSON_A)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_60_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected = Arrays.asList();

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void singleAttendeeOverlappingEvents() {
    // Have an overlapping event for a person. We should only see two options.
    //
    // Events  :       |--A--|
    //                     |--A--|
    // Day     : |---------------------|
    // Options : |--1--|         |--2--|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0830AM, DURATION_60_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_60_MINUTES),
            Arrays.asList(PERSON_A)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            TimeRange.fromStartEnd(TIME_1000AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void secondAttendeeWithoutEvents() {
    // Attendee without event should not affect options.
    //
    // Events  :       |--A--|
    //                     |--A--|
    // Day     : |---------------------|
    // Options : |--1--|         |--2--|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0830AM, DURATION_60_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_60_MINUTES),
            Arrays.asList(PERSON_A)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            TimeRange.fromStartEnd(TIME_1000AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void everyAttendeeIsConsideredWithMoreThanTwoAttendees() {
    // Have each person have different events. We should see two options because each person has
    // split the restricted times.
    //
    // Events  :       |--A--||--C--||--B--|
    // Day     : |--------------------------------|
    // Options : |--1--|                    |--3--|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_B)),
        new Event("Event 3", TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_C)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B, PERSON_C), DURATION_30_MINUTES);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void everyAttendeeIsConsideredWithBookedOptional() {
    // Two attendees with one optional that is completely booked.
    // We should see options as if the optinal attendee is not attending.
    //
    // Events  :       |--A--|     |--B--|
    // Optional: |~~~~~~~~~~~~~~C~~~~~~~~~~~~~~|
    // Day     : |-----------------------------|
    // Options : |--1--|     |--2--|     |--3--|
    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_B)),
        new Event("Event 3",
            TimeRange.fromStartDuration(TimeRange.START_OF_DAY, TimeRange.WHOLE_DAY.duration()),
            Arrays.asList(PERSON_C)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_C);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartEnd(TIME_0830AM, TIME_0900AM, false),
            TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void everyAttendeeIsConsideredWithMiddleEventOptional() {
    // Two attendees with one optional that is booked in the middle of the day.
    // We should see two options as if the optinal attendee is mandatory.
    //
    // Events  :       |--A--|     |--B--|
    // Optional:             |~~C~~|
    // Day     : |-----------------------------|
    // Options : |--1--|     |--2--|     |--3--|
    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_B)),
        new Event("Event 3", TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_C)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_C);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void justEnoughRoomWithBookedOptional() {
    // One attendee with one optional that is completely booked.
    // We should see options as if the optinal attendee is not attending.
    //
    // Events  : |--A--|     |----A----|
    // Optional: |~~~~~~~~~~B~~~~~~~~~~|
    // Day     : |---------------------|
    // Options :       |--1--|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartEnd(TIME_0900AM, TimeRange.END_OF_DAY, true),
            Arrays.asList(PERSON_A)),
        new Event("Event 3", TimeRange.fromStartDuration(TIME_0830AM, DURATION_15_MINUTES),
            Arrays.asList(PERSON_B)));

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_B);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void zeroMandatoryTwoOptionalWithEvents() {
    // Zero mandatory attendees with two optionals that are booked in the middle of the day.
    // We should see options as if the optinal attendee is not attending.
    //
    // Events  :
    // Optional:       |~~A~~|     |~~B~~|
    // Day     : |-----------------------------|
    // Options : |-----|     |-----|     |-----|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_B)));

    MeetingRequest request = new MeetingRequest(NO_ATTENDEES, DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_A);
    request.addOptionalAttendee(PERSON_B);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartEnd(TIME_0830AM, TIME_0900AM, false),
            TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void zeroMandatoryTwoBookedOptional() {
    // Zero mandatory attendees with two optionals that are booked for the whole day.
    // We should see the whole day open.
    //
    // Events  :
    // Optional: |~~~~~~~~~~~~~~A~~~~~~~~~~~~~~|
    // Optional: |~~~~~~~~~~~~~~B~~~~~~~~~~~~~~|
    // Day     : |-----------------------------|
    // Options : |-----------------------------|
    Collection<Event> events = Arrays.asList(
        new Event("Event 1",
            TimeRange.fromStartDuration(TimeRange.START_OF_DAY, TimeRange.WHOLE_DAY.duration()),
            Arrays.asList(PERSON_A)),
        new Event("Event 2",
            TimeRange.fromStartDuration(TimeRange.START_OF_DAY, TimeRange.WHOLE_DAY.duration()),
            Arrays.asList(PERSON_B)));

    MeetingRequest request = new MeetingRequest(NO_ATTENDEES, DURATION_1_HOUR);
    request.addOptionalAttendee(PERSON_A);
    request.addOptionalAttendee(PERSON_B);

    Collection<TimeRange> actual = query.query(NO_EVENTS, request);
    Collection<TimeRange> expected = Arrays.asList(TimeRange.WHOLE_DAY);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void MoreThanTwoAttendeesAndOneOptionalAttendee() {
    // Have each person have different events. We should see two options because each person has
    // split the restricted times.
    //
    // Events  :       |--A--||--C--||--B--|
    //                                      |~~D~~|
    // Day     : |--------------------------------------|
    // Options : |--1--|                          |--2--|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_B)),
        new Event("Event 3", TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_C)),
        new Event("Event 4", TimeRange.fromStartDuration(TIME_0930AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_D)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B, PERSON_C), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_D);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartEnd(TIME_1000AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void MoreThanTwoAttendeesAndMoreThanTwoOptionalAttendees() {
    // Have each person have different events. We should see two options because each person has
    // split the restricted times.
    //
    // Events  :       |--A--||--C--||--B--|
    // Optional:                           |~~D~~||~~E~~||~~F~~|
    // Day     : |---------------------------------------------------|
    // Options : |--1--|                                       |--2--|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_B)),
        new Event("Event 3", TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_C)),
        new Event("Event 4", TimeRange.fromStartDuration(TIME_0930AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_D)),
        new Event("Event 5", TimeRange.fromStartDuration(TIME_1000AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_E)),
        new Event("Event 6", TimeRange.fromStartDuration(TIME_1030AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_F)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B, PERSON_C), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_D);
    request.addOptionalAttendee(PERSON_E);
    request.addOptionalAttendee(PERSON_F);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartEnd(TIME_1100AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void MoreThanTwoAttendeesAndMoreThanTwoBookedOptionalAttendees() {
    // Have each person have different events. We should see two options because each person has
    // split the restricted times. Should see times as if the optional are not attending.
    //
    // Events  :       |--A--||--C--||--B--|
    // Optional: |~~~~~~~~~~~~~~~D~~~~~~~~~~~~~~~|
    // Optional: |~~~~~~~~~~~~~~~E~~~~~~~~~~~~~~~|
    // Optional: |~~~~~~~~~~~~~~~F~~~~~~~~~~~~~~~|
    // Day     : |-------------------------------|
    // Options : |--1--|                   |--2--|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_B)),
        new Event("Event 3", TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_C)),
        new Event("Event 5",
            TimeRange.fromStartDuration(TimeRange.START_OF_DAY, TimeRange.WHOLE_DAY.duration()),
            Arrays.asList(PERSON_D, PERSON_E, PERSON_F)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B, PERSON_C), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_D);
    request.addOptionalAttendee(PERSON_E);
    request.addOptionalAttendee(PERSON_F);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void MoreThanTwoAttendeesAndMoreThanTwoOptionalAttendeesWithSomeFreeAndSomeNot() {
    // Have each person have different events. We should see two options because each person has
    // split the restricted times. The optional that can attend should be able to. The ones that
    // are not able to, should not be considered.
    //
    // Events  :       |--A--||--C--||--B--|
    // Optional:                        |~~D~~|
    // Optional: |~~~~~~~~~~~~~~~~~~E~~~~~~~~~~~~~~~~|
    // Optional: |~~~~~~~~~~~~~~~~~~F~~~~~~~~~~~~~~~~|
    // Day     : |-----------------------------------|
    // Options : |--1--|                       |--2--|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_B)),
        new Event("Event 3", TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_C)),
        new Event("Event 4", TimeRange.fromStartDuration(TIME_0915AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_D)),
        new Event("Event 5",
            TimeRange.fromStartDuration(TimeRange.START_OF_DAY, TimeRange.WHOLE_DAY.duration()),
            Arrays.asList(PERSON_E, PERSON_F)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B, PERSON_C), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_D);
    request.addOptionalAttendee(PERSON_E);
    request.addOptionalAttendee(PERSON_F);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartEnd(TIME_0945AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void TwoOutOfThreeOptionalAttendeesCanAttendWithOverlappingTimes() {
    // Have each person have different events. We should see two options because each person has
    // split the restricted times. The optionals that can attend should be able to. The one that
    // is not able to, should not be considered.
    //
    // Events  :       |--A--||--C--||--B--|
    // Optional:                        |~~D~~|
    // Optional:                        |~~E~~|
    // Optional: |~~~~~~~~~~~~~~~~~~F~~~~~~~~~~~~~~~~|
    // Day     : |-----------------------------------|
    // Options : |--1--|                       |--2--|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_B)),
        new Event("Event 3", TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_C)),
        new Event("Event 4", TimeRange.fromStartDuration(TIME_0915AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_D, PERSON_E)),
        new Event("Event 5",
            TimeRange.fromStartDuration(TimeRange.START_OF_DAY, TimeRange.WHOLE_DAY.duration()),
            Arrays.asList(PERSON_F)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B, PERSON_C), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_D);
    request.addOptionalAttendee(PERSON_E);
    request.addOptionalAttendee(PERSON_F);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartEnd(TIME_0945AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void FindTimeWhereMostOptionalsCanAttend() {
    // We should see two options because each person can attend at the start and at the end
    //
    // Events  :       |--A--||--C--||--B--|
    // Optional:                           |~~D~~|
    // Optional:                           |~~E~~|
    // Optional:                                 |~~F~~|
    // Day     : |-------------------------------------------|
    // Options : |--1--|                               |--2--|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_B)),
        new Event("Event 3", TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_C)),
        new Event("Event 4", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_D, PERSON_E)),
        new Event("Event 5", TimeRange.fromStartDuration(TIME_0930AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_F)));

    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B, PERSON_C), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_D);
    request.addOptionalAttendee(PERSON_E);
    request.addOptionalAttendee(PERSON_F);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartEnd(TIME_1000AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void FindTimeWhereMostOptionalsCanAttendWithDifferingAvailabilites() {
    // Two optionals can attend at a certain time, one cannot. There is no time where all can
    // attend. Thus, the time when the two can attend should be returned.
    //
    // Optional: |~~A~~|     |~~~~~~~~~~~~A~~~~~~~~~~~~|
    // Optional:       |~~B~~|
    // Optional: |~~C~~|     |~~~~~~~~~~~~C~~~~~~~~~~~~|
    // Day     : |--------------------------------------|
    // Options :       |--1--|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            Arrays.asList(PERSON_A, PERSON_C)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_B)),
        new Event("Event 4", TimeRange.fromStartEnd(TIME_0900AM, TimeRange.END_OF_DAY, true),
            Arrays.asList(PERSON_A, PERSON_C)));

    MeetingRequest request = new MeetingRequest(NO_ATTENDEES, DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_A);
    request.addOptionalAttendee(PERSON_B);
    request.addOptionalAttendee(PERSON_C);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TIME_0830AM, TIME_0900AM, false));

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void FindTimeWhereMostOptionalsCanAttendWithATie() {
    // There are three times at which two optionals can attend and two cannot.
    // All of these times should be returned.
    //
    // Optional: |~~A~~|       |~~~~~~~~~~~~~A~~~~~~~~~~~~|
    // Optional:        |~~B~~|
    // Optional: |~~C~~|       |~~~~~~~~~~~~~C~~~~~~~~~~~~|
    // Optional:        |~~D~~|
    // Day     : |----------------------------------------|
    // Options : |--1--||--2--||-----------3--------------|

    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            Arrays.asList(PERSON_A, PERSON_C)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_B, PERSON_D)),
        new Event("Event 4", TimeRange.fromStartEnd(TIME_0900AM, TimeRange.END_OF_DAY, true),
            Arrays.asList(PERSON_A, PERSON_C)));

    MeetingRequest request = new MeetingRequest(NO_ATTENDEES, DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_A);
    request.addOptionalAttendee(PERSON_B);
    request.addOptionalAttendee(PERSON_C);
    request.addOptionalAttendee(PERSON_D);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            TimeRange.fromStartEnd(TIME_0830AM, TIME_0900AM, false),
            TimeRange.fromStartEnd(TIME_0900AM, TimeRange.END_OF_DAY, true));

    Assert.assertEquals(expected, actual);
  }
}
