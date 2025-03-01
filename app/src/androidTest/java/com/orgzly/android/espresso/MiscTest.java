package com.orgzly.android.espresso;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.DrawerActions.open;
import static androidx.test.espresso.contrib.PickerActions.setDate;
import static androidx.test.espresso.contrib.PickerActions.setTime;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.util.EspressoUtils.clickClickableSpan;
import static com.orgzly.android.espresso.util.EspressoUtils.clickSetting;
import static com.orgzly.android.espresso.util.EspressoUtils.isHighlighted;
import static com.orgzly.android.espresso.util.EspressoUtils.onActionItemClick;
import static com.orgzly.android.espresso.util.EspressoUtils.onBook;
import static com.orgzly.android.espresso.util.EspressoUtils.onListItem;
import static com.orgzly.android.espresso.util.EspressoUtils.onNoteInBook;
import static com.orgzly.android.espresso.util.EspressoUtils.onPreface;
import static com.orgzly.android.espresso.util.EspressoUtils.onSavedSearch;
import static com.orgzly.android.espresso.util.EspressoUtils.replaceTextCloseKeyboard;
import static com.orgzly.android.espresso.util.EspressoUtils.scroll;
import static com.orgzly.android.espresso.util.EspressoUtils.searchForTextCloseKeyboard;
import static com.orgzly.android.espresso.util.EspressoUtils.settingsSetDoneKeywords;
import static com.orgzly.android.espresso.util.EspressoUtils.settingsSetTodoKeywords;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.SystemClock;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TimePicker;

import androidx.test.core.app.ActivityScenario;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.RetryTestRule;
import com.orgzly.android.db.entity.NotePosition;
import com.orgzly.android.espresso.util.EspressoUtils;
import com.orgzly.android.repos.RepoType;
import com.orgzly.android.ui.main.MainActivity;
import com.orgzly.android.ui.repos.ReposActivity;

import org.hamcrest.Matcher;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;


public class MiscTest extends OrgzlyTest {

    @Rule
    public RetryTestRule mRetryTestRule = new RetryTestRule();

    @Test
    public void testLftRgt() {
        testUtils.setupBook("booky", "Preface\n* Note 1\n** Note 2\n* Note 3\n");

        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            NotePosition n1 = dataRepository.getLastNote("Note 1").getPosition();
            NotePosition n2 = dataRepository.getLastNote("Note 2").getPosition();
            NotePosition n3 = dataRepository.getLastNote("Note 3").getPosition();

            assertTrue(n1.getLft() < n2.getLft());
            assertTrue(n2.getLft() < n2.getRgt());
            assertTrue(n2.getRgt() < n1.getRgt());
            assertTrue(n1.getRgt() < n3.getLft());
            assertTrue(n3.getLft() < n3.getRgt());
        }
    }

    @Test
    public void testClearDatabaseWithFragmentsInBackStack() {
        testUtils.setupBook(
                "book-one",
                "First book used for testing\n" +
                        "* Note A.\n" +
                        "** Note B.\n");

        testUtils.setupBook(
                "book-two",
                "Sample book used for tests\n" +
                        "* Note #1.\n" +
                        "* Note #2.\n" +
                        "** TODO Note #3.\n");

        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.fragment_books_view_flipper)).check(matches(isDisplayed()));

            onBook(0).perform(click());
            onView(withText("Note B.")).perform(click());
            onView(withId(R.id.drawer_layout)).perform(open());
            onView(allOf(withText("book-two"), isDisplayed())).perform(click());
            onView(withText("Note #2.")).perform(click());
            onActionItemClick(R.id.activity_action_settings, R.string.settings);
            clickSetting(R.string.app);
            clickSetting(R.string.clear_database);
            onView(withText(R.string.ok)).perform(click());
            pressBack();
            pressBack();

            onView(withId(R.id.fragment_books_view_flipper)).check(matches(isDisplayed()));
            onView(withText(R.string.no_notebooks)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testClickOnListViewItemOutOfView() {
        testUtils.setupBook("book-one", "Sample book used for tests\n* 1\n* 2\n* 3\n* 4\n* 5\n* 6\n* 7\n* 8\n* 9\n* 10\n* 11\n* 12\n* 13\n* 14\n* 15\n");
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onBook(0).perform(click());
            onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));
            onNoteInBook(15, R.id.item_head_title_view)
                    .check(matches(allOf(withText("15"), isDisplayed())));
        }
    }

    @Test
    public void testChangingNoteStatesToDone() {
        testUtils.setupBook("book-name",
                "Sample book used for tests\n" +
                        "* Note #1.\n" +
                        "* Note #2.\n" +
                        "** TODO Note #3.\n" +
                        "** Note #4.\n" +
                        "*** DONE Note #5.\n" +
                        "CLOSED: [2014-06-03 Tue 13:34]\n" +
                        "");
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(allOf(withText("book-name"), isDisplayed())).perform(click());

            onNoteInBook(1, R.id.item_head_title_view).check(matches(allOf(withText("Note #1."), isDisplayed())));
            onNoteInBook(2, R.id.item_head_title_view).check(matches(allOf(withText("Note #2."), isDisplayed())));
            onNoteInBook(3, R.id.item_head_title_view).check(matches(allOf(withText("TODO  Note #3."), isDisplayed())));
            onNoteInBook(4, R.id.item_head_title_view).check(matches(allOf(withText("Note #4."), isDisplayed())));
            onNoteInBook(5, R.id.item_head_title_view).check(matches(allOf(withText("DONE  Note #5."), isDisplayed())));

            onNoteInBook(1).perform(longClick());
            onNoteInBook(2).perform(click());
            onNoteInBook(3).perform(click());
            onView(withId(R.id.state)).perform(click());
            onView(withText("DONE")).perform(click());

            onNoteInBook(1, R.id.item_head_title_view).check(matches(allOf(withText("DONE  Note #1."), isDisplayed())));
            onNoteInBook(1, R.id.item_head_closed_text).check(matches(isDisplayed()));
            onNoteInBook(1).check(matches(isHighlighted()));

            onNoteInBook(2, R.id.item_head_title_view).check(matches(allOf(withText("DONE  Note #2."), isDisplayed())));
            onNoteInBook(2, R.id.item_head_closed_text).check(matches(isDisplayed()));
            onNoteInBook(2).check(matches(isHighlighted()));

            onNoteInBook(3, R.id.item_head_title_view).check(matches(allOf(withText("DONE  Note #3."), isDisplayed())));
            onNoteInBook(3, R.id.item_head_closed_text).check(matches(isDisplayed()));
            onNoteInBook(3).check(matches(isHighlighted()));

            onNoteInBook(4, R.id.item_head_title_view).check(matches(allOf(withText("Note #4."), isDisplayed())));
            onNoteInBook(4, R.id.item_head_closed_text).check(matches(not(isDisplayed())));
            onNoteInBook(4).check(matches(not(isHighlighted())));

            onNoteInBook(5, R.id.item_head_title_view).check(matches(allOf(withText("DONE  Note #5."), isDisplayed())));
            onNoteInBook(5, R.id.item_head_closed_text).check(matches(isDisplayed()));
            onNoteInBook(5).check(matches(not(isHighlighted())));
        }
    }

    @Test
    public void testSchedulingMultipleNotes() {
        testUtils.setupBook("book-name",
                "Sample book used for tests\n" +
                        "* Note #1.\n" +
                        "* Note #2.\n" +
                        "** TODO Note #3.\n" +
                        "** Note #4.\n" +
                        "*** DONE Note #5.\n" +
                        "CLOSED: [2014-06-03 Tue 13:34]\n" +
                        "");
        EspressoUtils.grantAlarmsAndRemindersSpecialPermission();
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(allOf(withText("book-name"), isDisplayed())).perform(click());

            onNoteInBook(1, R.id.item_head_scheduled_text).check(matches(not(isDisplayed())));
            onNoteInBook(2, R.id.item_head_scheduled_text).check(matches(not(isDisplayed())));
            onNoteInBook(3, R.id.item_head_scheduled_text).check(matches(not(isDisplayed())));
            onNoteInBook(4, R.id.item_head_scheduled_text).check(matches(not(isDisplayed())));
            onNoteInBook(5, R.id.item_head_scheduled_text).check(matches(not(isDisplayed())));

            onNoteInBook(1).perform(longClick());
            onNoteInBook(2).perform(click());
            onNoteInBook(3).perform(click());
            onView(withId(R.id.schedule)).perform(click());
            onView(withId(R.id.date_picker_button)).perform(click());
            onView(withClassName(equalTo(DatePicker.class.getName()))).perform(setDate(2014, 4, 1));
            onView(withText(android.R.string.ok)).perform(click());
            onView(withId(R.id.time_picker_button)).perform(scroll(), click());
            onView(withClassName(equalTo(TimePicker.class.getName()))).perform(setTime(9, 15));
            onView(withText(android.R.string.ok)).perform(click());
            onView(withText(R.string.set)).perform(click());

            onNoteInBook(1, R.id.item_head_scheduled_text).check(matches(allOf(isDisplayed(), withText(userDateTime("<2014-04-01 Tue 09:15>")))));
            onNoteInBook(2, R.id.item_head_scheduled_text).check(matches(allOf(isDisplayed(), withText(userDateTime("<2014-04-01 Tue 09:15>")))));
            onNoteInBook(3, R.id.item_head_scheduled_text).check(matches(allOf(isDisplayed(), withText(userDateTime("<2014-04-01 Tue 09:15>")))));
            onNoteInBook(4, R.id.item_head_scheduled_text).check(matches(not(isDisplayed())));
            onNoteInBook(5, R.id.item_head_scheduled_text).check(matches(not(isDisplayed())));
        }
    }

    @Test
    public void testTrimmingTitleInNoteFragment() {
        testUtils.setupBook("book-one", "Sample book used for tests\n* 1\n* 2\n* 3\n");
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onBook(0).perform(click());
            onView(withId(R.id.fab)).perform(click());

            onView(withId(R.id.title_edit))
                    .perform(replaceTextCloseKeyboard("    Title with empty spaces all around   "));
            onView(withId(R.id.done)).perform(click()); // Note done
            onNoteInBook(4, R.id.item_head_title_view).check(matches(withText("Title with empty spaces all around")));
        }
    }

    @Test
    public void testNewBookDialogShouldSurviveScreenRotation() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity ->
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
            SystemClock.sleep(500);
            onView(withId(R.id.fab)).perform(click());
            onView(withId(R.id.dialog_new_book_container)).check(matches(isDisplayed()));

            scenario.onActivity(activity ->
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));

            SystemClock.sleep(1000);
            onView(withId(R.id.dialog_new_book_container)).check(matches(isDisplayed()));
            onView(withId(R.id.dialog_input)).perform(replaceTextCloseKeyboard("notebook"));
            onView(withText(R.string.create)).perform(click());

            onView(allOf(withText("notebook"), isDisplayed())).perform(click());

            onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));
        }
    }

    /**
     * There was a race condition. Old title is displayed if drawer
     * is closed after book has been loaded.
     */
    @Test
    public void testBookTitleMustBeDisplayedWhenOpeningBookFromDrawer() {
        testUtils.setupBook("book-one", "Sample book used for tests\n* 1\n* 2\n* 3\n");
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.drawer_layout)).perform(open());
            onView(allOf(withText("book-one"), isDescendantOfA(withId(R.id.drawer_navigation_view)))).perform(click());
            onView(allOf(withText("book-one"), isDescendantOfA(withId(R.id.top_toolbar)))).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testTimestampDialogTimeButtonValueWhenToggling() {
        EspressoUtils.grantAlarmsAndRemindersSpecialPermission();
        testUtils.setupBook("book-name", "Sample book used for tests\n" +
                "* TODO Note #1.\n" +
                "SCHEDULED: <2015-01-18 04:05 +6d>\n" +
                "* Note #2.\n" +
                "");
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onBook(0).perform(click());

            onNoteInBook(1).perform(click());

            String regex = "4:05\\sAM";

            onView(withId(R.id.scheduled_button)).perform(click());
            onView(withId(R.id.time_picker_button)).check(matches(withText(matchesPattern(regex))));
            onView(withId(R.id.time_used_checkbox)).perform(scroll(), click());
            onView(withId(R.id.time_picker_button)).check(matches(withText(matchesPattern(regex))));
            onView(withId(R.id.time_used_checkbox)).perform(click());
            onView(withId(R.id.time_picker_button)).check(matches(withText(matchesPattern(regex))));
        }
    }

    @Test
    public void testTimestampComplicated() {
        testUtils.setupBook("book-name",
                "Sample book used for tests\n" +
                        "* Note #1.\n" +
                        "SCHEDULED: <2015-01-18 04:05 .+6d>\n" +
                        "* Note #2.\n" +
                        "");
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(allOf(withText("book-name"), isDisplayed())).perform(click());

            onNoteInBook(1, R.id.item_head_scheduled_text).check(matches(allOf(isDisplayed(), withText(userDateTime("<2015-01-18 Sun 04:05 .+6d>")))));
        }
    }

    @Test
    public void testScheduledWithRepeaterToDoneFromBook() {
        testUtils.setupBook("book-name", "Sample book used for tests\n" +
                "* TODO Note #1.\n" +
                "SCHEDULED: <2015-01-18 04:05 +6d>\n" +
                "* Note #2.\n" +
                "");
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            settingsSetDoneKeywords("DONE OLD");

            onBook(0).perform(click());

            onNoteInBook(1).perform(longClick());

            /* TO DO -> DONE */
            onView(withId(R.id.state)).perform(click());
            onView(withText("DONE")).perform(click());
            onNoteInBook(1, R.id.item_head_title_view).check(matches(withText(startsWith("TODO"))));
            onNoteInBook(1, R.id.item_head_closed_text).check(matches(not(isDisplayed())));
            onNoteInBook(1, R.id.item_head_scheduled_text).check(matches(withText(userDateTime("<2015-01-24 Sat 04:05 +6d>"))));

            /* DONE -> NOTE */
            onView(withId(R.id.state)).perform(click());
            onView(withText(R.string.clear)).perform(click());
            onNoteInBook(1, R.id.item_head_title_view).check(matches(withText(startsWith("Note"))));
            onNoteInBook(1, R.id.item_head_closed_text).check(matches(not(isDisplayed())));
            onNoteInBook(1, R.id.item_head_scheduled_text).check(matches(withText(userDateTime("<2015-01-24 Sat 04:05 +6d>"))));

            /* NOTE -> DONE */
            onView(withId(R.id.state)).perform(click());
            onView(withText("DONE")).perform(click());
            onNoteInBook(1, R.id.item_head_title_view).check(matches(withText(startsWith("Note"))));
            onNoteInBook(1, R.id.item_head_closed_text).check(matches(not(isDisplayed())));
            onNoteInBook(1, R.id.item_head_scheduled_text).check(matches(withText(userDateTime("<2015-01-30 Fri 04:05 +6d>"))));

            /* NOTE -> OLD */
            onView(withId(R.id.state)).perform(click());
            onView(withText("OLD")).perform(click());
            onNoteInBook(1, R.id.item_head_title_view).check(matches(withText(startsWith("Note"))));
            onNoteInBook(1, R.id.item_head_closed_text).check(matches(not(isDisplayed())));
            onNoteInBook(1, R.id.item_head_scheduled_text).check(matches(withText(userDateTime("<2015-02-05 Thu 04:05 +6d>"))));
        }
    }

    @Test
    public void testScheduledWithRepeaterToDoneFromNoteFragment() {
        testUtils.setupBook("book-name",
                "Sample book used for tests\n" +
                        "* TODO Note #1.\n" +
                        "SCHEDULED: <2015-01-18 04:05 +6d>\n" +
                        "* Note #2.\n" +
                        "");
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            settingsSetDoneKeywords("DONE OLD");

            SystemClock.sleep(500);
            onView(allOf(withText("book-name"), isDisplayed())).perform(click());

            onNoteInBook(1).perform(click());

            /* TO DO -> DONE */
            onView(withId(R.id.state_button)).perform(click());
            onView(withText("DONE")).perform(click());

            onView(withId(R.id.closed_button)).check(matches(not(isDisplayed())));
            onView(withId(R.id.scheduled_button)).check(matches(withText(userDateTime("<2015-01-24 Sat 04:05 +6d>"))));

            /* DONE -> NOTE */
            onView(withId(R.id.state_button)).perform(click());
            onView(withText(R.string.clear)).perform(click());
            onView(withId(R.id.closed_button)).check(matches(not(isDisplayed())));
            onView(withId(R.id.scheduled_button)).check(matches(withText(userDateTime("<2015-01-24 Sat 04:05 +6d>"))));

            /* NOTE -> DONE */
            onView(withId(R.id.state_button)).perform(click());
            onView(withText("DONE")).perform(click());
            onView(withId(R.id.closed_button)).check(matches(not(isDisplayed())));
            onView(withId(R.id.scheduled_button)).check(matches(withText(userDateTime("<2015-01-30 Fri 04:05 +6d>"))));

            /* NOTE -> OLD */
            onView(withId(R.id.state_button)).perform(click());
            onView(withText("OLD")).perform(click());
            onView(withId(R.id.closed_button)).check(matches(not(isDisplayed())));
            onView(withId(R.id.scheduled_button)).check(matches(withText(userDateTime("<2015-02-05 Thu 04:05 +6d>"))));
        }
    }

    @Test
    public void testSettingStateToTodo() {
        testUtils.setupBook("booky", "* TODO Note 1\n* Note 2\n* Note 3\n* Note 4\n* TODO Note 5");
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(allOf(withText("booky"), isDisplayed())).perform(click());

            onNoteInBook(1, R.id.item_head_title_view).check(matches(allOf(withText("TODO  Note 1"), isDisplayed())));
            onNoteInBook(2, R.id.item_head_title_view).check(matches(allOf(withText("Note 2"), isDisplayed())));
            onNoteInBook(3, R.id.item_head_title_view).check(matches(allOf(withText("Note 3"), isDisplayed())));
            onNoteInBook(4, R.id.item_head_title_view).check(matches(allOf(withText("Note 4"), isDisplayed())));
            onNoteInBook(5, R.id.item_head_title_view).check(matches(allOf(withText("TODO  Note 5"), isDisplayed())));

            onNoteInBook(1).perform(longClick());
            onNoteInBook(2).perform(click());
            onNoteInBook(3).perform(click());
            onView(withId(R.id.state)).perform(click());
            onView(withText("TODO")).perform(click());

            onNoteInBook(1, R.id.item_head_title_view).check(matches(allOf(withText("TODO  Note 1"), isDisplayed())));
            onNoteInBook(2, R.id.item_head_title_view).check(matches(allOf(withText("TODO  Note 2"), isDisplayed())));
            onNoteInBook(3, R.id.item_head_title_view).check(matches(allOf(withText("TODO  Note 3"), isDisplayed())));
            onNoteInBook(4, R.id.item_head_title_view).check(matches(allOf(withText("Note 4"), isDisplayed())));
            onNoteInBook(5, R.id.item_head_title_view).check(matches(allOf(withText("TODO  Note 5"), isDisplayed())));
        }
    }

    /**
     * Visits every fragment used in the main activity and calls {@link #fragmentTest} on it.
     */

    private Activity activity;
    @Test
    public void testMainActivityFragments() {
        testUtils.setupRepo(RepoType.DIRECTORY, "file:/");
        testUtils.setupRepo(RepoType.DROPBOX, "dropbox:/orgzly");
        testUtils.setupBook("book-one", "Preface\n\n* Note");

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(it -> activity = it);

            // Books
            // fragmentTest(activity, true, withId(R.id.fragment_books_view_flipper));

            // Book
            onBook(0).perform(click());
            fragmentTest(activity, true, withId(R.id.fragment_book_view_flipper));

            // Note
            onView(withText("Note")).perform(click());
            fragmentTest(activity, false, withId(R.id.scroll_view));
            pressBack();

            // Preface
            onPreface().perform(click());
            fragmentTest(activity, false, withId(R.id.fragment_book_preface_container));
            pressBack();
            pressBack();

            // Opened drawer
            onView(withId(R.id.drawer_layout)).perform(open());
            fragmentTest(activity, true, withText(R.string.searches));

            // Saved searches
            onView(withId(R.id.drawer_layout)).perform(open());
            onView(withText(R.string.searches)).perform(click());
            fragmentTest(activity, true, withId(R.id.fragment_saved_searches_flipper));

            // Search
            onSavedSearch(0).perform(click());
            fragmentTest(activity, false, withId(R.id.fragment_saved_search_flipper));

            // Search results
            onView(withId(R.id.drawer_layout)).perform(open());
            SystemClock.sleep(500);
            onView(withText("Scheduled")).perform(click());
            fragmentTest(activity, true, withId(R.id.fragment_query_search_view_flipper));

            // Agenda
            searchForTextCloseKeyboard("t.tag3 ad.3");
            fragmentTest(activity, true, withId(R.id.fragment_query_agenda_view_flipper));
        }
    }

    @Test
    public void testReposActivityFragments() {
        Assume.assumeTrue(BuildConfig.IS_DROPBOX_ENABLED);

        testUtils.setupRepo(RepoType.DIRECTORY, "file:/");
        testUtils.setupRepo(RepoType.DROPBOX, "dropbox:/orgzly");
        testUtils.setupBook("book-one", "Preface\n\n* Note");

        try (ActivityScenario<ReposActivity> scenario = ActivityScenario.launch(ReposActivity.class)) {
            scenario.onActivity(it -> activity = it);

            // List of repos
            fragmentTest(activity, false, withId(R.id.activity_repos_flipper));

            // Directory repo
            onListItem(1).perform(click());
            fragmentTest(activity, false, withId(R.id.activity_repo_directory_container));
            pressBack();

            // Dropbox repo
            onListItem(0).perform(click());
            fragmentTest(activity, false, withId(R.id.activity_repo_dropbox_container));
        }
    }

    private void fragmentTest(Activity activity, boolean hasSearchMenuItem, Matcher<View> matcher) {
        SystemClock.sleep(500);
        onView(matcher).check(matches(isDisplayed()));

        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        SystemClock.sleep(500);
        onView(matcher).check(matches(isDisplayed()));
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        SystemClock.sleep(500);
        onView(matcher).check(matches(isDisplayed()));
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        SystemClock.sleep(500);
        onView(matcher).check(matches(isDisplayed()));
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        SystemClock.sleep(500);
        onView(matcher).check(matches(isDisplayed()));
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        SystemClock.sleep(500);

        if (hasSearchMenuItem) {
            onView(withId(R.id.search_view)).check(matches(isDisplayed()));
        } else {
            onView(withId(R.id.search_view)).check(doesNotExist());
        }
    }

    @Test
    public void testBookTitleFromInBufferSettingsDisplayed() {
        testUtils.setupBook("book-name", "#+TITLE: Notebook Title\n* TODO Note #1.\n");
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            /* Books fragment. */
            onBook(0, R.id.item_book_title).check(matches(withText("Notebook Title")));
            onBook(0, R.id.item_book_subtitle).check(matches(withText("book-name")));

            /* Books in drawer. */
            onView(withId(R.id.drawer_layout)).perform(open());
            onView(allOf(withText("Notebook Title"), isDescendantOfA(withId(R.id.drawer_navigation_view)))).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testBookTitleSettingIsPartOfPreface() {
        testUtils.setupBook("book-name", "#+TITLE: Notebook Title\n* TODO Note #1.\n");
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(allOf(withText("Notebook Title"), isDisplayed())).perform(click());
            onNoteInBook(0, R.id.item_preface_text_view)
                    .check(matches(withText(containsString("#+TITLE: Notebook Title"))));

        }
    }

    @Test
    public void testBookTitleChangeOnPrefaceEdit() {
        testUtils.setupBook("book-name", "* TODO Note #1.\n");
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onBook(0, R.id.item_book_title).check(matches(withText("book-name")));

            /* Set #+TITLE */
            onView(allOf(withText("book-name"), isDisplayed())).perform(click());
            onActionItemClick(R.id.books_options_menu_book_preface, R.string.edit_book_preface);
            onView(withId(R.id.fragment_book_preface_content)).perform(click());
            onView(withId(R.id.fragment_book_preface_content_edit))
                    .perform(replaceTextCloseKeyboard("#+TITLE: Notebook Title"));
            onView(withId(R.id.done)).perform(click()); // Preface done
            pressBack();

            onBook(0, R.id.item_book_title).check(matches(withText("Notebook Title")));
        }
    }

    @Test
    public void testBookTitleRemoving() {
        testUtils.setupBook("book-name", "#+TITLE: Notebook Title\n* TODO Note #1.\n");
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onBook(0, R.id.item_book_title).check(matches(withText("Notebook Title")));
            onBook(0, R.id.item_book_subtitle).check(matches(withText("book-name")));

            onBook(0).perform(click());
            onPreface().perform(click());
            onView(withId(R.id.fragment_book_preface_content)).perform(click());
            onView(withId(R.id.fragment_book_preface_content_edit)).perform(replaceTextCloseKeyboard("#+TTL: Notebook Title"));
            onView(withId(R.id.done)).perform(click()); // Preface done
            onNoteInBook(0, R.id.item_preface_text_view)
                    .check(matches(withText(containsString("#+TTL: Notebook Title"))));
            pressBack();

            onBook(0, R.id.item_book_title).check(matches(withText("book-name")));
            onBook(0, R.id.item_book_subtitle).check(matches(not(isDisplayed())));
        }
    }

    @Test
    public void testBookReparseOnStateConfigChange() {
        testUtils.setupBook("book-name",
                "Sample book used for tests\n" +
                        "* Note #1.\n" +
                        "* Note #2.\n" +
                        "SCHEDULED: <2014-05-22 Thu> DEADLINE: <2014-05-22 Thu>\n" +
                        "** TODO Note #3.\n" +
                        "** Note #4.\n" +
                        "*** DONE Note #5.\n" +
                        "CLOSED: [2014-01-01 Tue 20:07]\n" +
                        "**** Note #6.\n" +
                        "** Note #7.\n" +
                        "* ANTIVIVISECTIONISTS Note #8.\n" +
                        "**** Note #9.\n" +
                        "** Note #10.\n" +
                        "");
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onBook(0).perform(click());
            onNoteInBook(8, R.id.item_head_title_view)
                    .check(matches(withText(startsWith("ANTIVIVISECTIONISTS "))))
                    .perform(click());
            onView(withId(R.id.title_view)).check(matches(withText("ANTIVIVISECTIONISTS Note #8.")));
            settingsSetTodoKeywords("TODO ANTIVIVISECTIONISTS");
            /* Must go to books and back, or the click below will not work for some reason. */
            pressBack(); // Leave book
            onView(allOf(withText("book-name"), isDisplayed())).perform(click());
            onNoteInBook(8).perform(click());
            onView(withId(R.id.title_view)).check(matches(withText("Note #8.")));
        }
    }

    @Test
    public void testCabStaysOpenWhenSelectingTheSameBookFromDrawer() {
        testUtils.setupBook("booky", "* TODO Note 1\n* Note 2\n* Note 3\n* Note 4\n* TODO Note 5");
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onBook(0).perform(click());
            onNoteInBook(3).perform(longClick());
            onActionItemClick(R.id.move, R.string.move);
            onView(withId(R.id.drawer_layout)).perform(open());
            onView(allOf(withText("booky"), isDescendantOfA(withId(R.id.drawer_navigation_view)))).perform(click());
            onView(withId(R.id.notes_action_move_left)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testNewlyCreatedBookShouldNotHaveEncodingsDisplayed() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.fab)).perform(click());
            onView(withId(R.id.dialog_input)).perform(replaceTextCloseKeyboard("booky"));
            onView(withText(R.string.create)).perform(click());
            onBook(0, R.id.item_book_encoding_used_container).check(matches(not(isDisplayed())));
            onBook(0, R.id.item_book_encoding_detected_container).check(matches(not(isDisplayed())));
            onBook(0, R.id.item_book_encoding_selected_container).check(matches(not(isDisplayed())));
        }
    }

    @Test
    public void testSelectingNoteThenOpeningAnotherBook() {
        testUtils.setupBook("booky-one", "* TODO Note 1\n* Note 2\n* Note 3\n* Note 4\n* TODO Note 5");
        testUtils.setupBook("booky-two", "* TODO Note A\n* Note B\n* Note C");
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onBook(0).perform(click());
            onNoteInBook(1).perform(longClick());

            onView(withId(R.id.toggle_state)).check(matches(isDisplayed()));

            onView(withId(R.id.drawer_layout)).perform(open());
            onView(allOf(withText("booky-two"), isDescendantOfA(withId(R.id.drawer_navigation_view)))).perform(click());
            onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));

            onView(withId(R.id.toggle_state)).check(doesNotExist());
        }
    }

    @Test
    public void testOpenBookAlreadyInBackStack() {
        testUtils.setupBook("booky-one", "* TODO Note 1\n* Note 2\n* Note 3\n* Note 4\n* TODO Note 5");
        testUtils.setupBook("booky-two", "* TODO Note A\n* Note B\n* Note C");
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onBook(0).perform(click());
            onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));
            onNoteInBook(1).perform(click());
            onView(withId(R.id.scroll_view)).check(matches(isDisplayed()));
            onView(withId(R.id.drawer_layout)).perform(open());
            onView(allOf(withText("booky-one"), isDescendantOfA(withId(R.id.drawer_navigation_view)))).perform(click());
            onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testCheckboxInTitle() {
        testUtils.setupBook("book-name", "* - [ ] Checkbox");
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onBook(0).perform(click());
            onNoteInBook(1, R.id.item_head_title_view).perform(clickClickableSpan("[ ]"));
        }
    }

    @Test
    public void testCheckboxWithFoldedDrawerBeforeIt() {
        testUtils.setupBook("book-name", ":DRAWER:\ndrawer\n:END:\n\n- [ ] Item");
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onBook(0).perform(click());

            onView(withId(R.id.item_preface_text_view)).perform(clickClickableSpan("[ ]"));

            SystemClock.sleep(500);
            onView(allOf(withId(R.id.item_preface_text_view), withText(containsString("- [X] Item"))))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void testClickingCheckboxInNoteDetails() {
        testUtils.setupBook("book-name", "* Title\n\n- [ ] Item");
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onBook(0).perform(click());
            onNoteInBook(1).perform(click());

            onView(withId(R.id.content_view)).perform(clickClickableSpan("[ ]"));

            onView(allOf(withId(R.id.content_view), withText(containsString("- [X] Item"))))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void testDrawerWithFoldedDrawerBeforeIt() {
        testUtils.setupBook("book-name", ":DRAWER1:\ndrawer1\n:END:\n\n:DRAWER2:\ndrawer2\n:END:");
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onBook(0).perform(click());

            onView(withId(R.id.item_preface_text_view)).perform(clickClickableSpan("DRAWER2"));
            onView(withId(R.id.item_preface_text_view)).perform(clickClickableSpan("DRAWER1"));
        }
    }

    @Test
    public void testActiveDrawerItemForSearchQuery() {
        testUtils.setupBook("booky-one", "* TODO Note 1\n* Note 2\n* Note 3\n* Note 4\n* TODO Note 5");
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onBook(0).perform(click());

            searchForTextCloseKeyboard("note");

            onView(withId(R.id.drawer_layout)).perform(open());
            onView(allOf(withText("booky-one"), isDescendantOfA(withId(R.id.drawer_navigation_view))))
                    .check(matches(not(isChecked())));
        }
    }
}
