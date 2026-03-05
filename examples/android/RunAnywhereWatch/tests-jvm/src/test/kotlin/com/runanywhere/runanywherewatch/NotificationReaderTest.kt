package com.runanywhere.runanywherewatch

import org.junit.Test
import org.junit.Before
import org.junit.Assert.*

/**
 * Pure JVM tests for notification reader and summarizer — no Android SDK dependencies.
 * Tests NotificationManager, WatchNotification, NotificationSummarizer, and QuickReplyGenerator.
 */

// ==================== DATA CLASSES ====================

enum class Priority {
    LOW, MEDIUM, HIGH, URGENT
}

data class WatchNotification(
    val id: String,
    val appName: String,
    val title: String,
    val content: String,
    val timestamp: Long,
    val priority: Priority,
    var isRead: Boolean = false
)

// ==================== NOTIFICATION MANAGER ====================

class NotificationManager {
    private val notifications = mutableListOf<WatchNotification>()
    private val unreadNotifications = mutableListOf<WatchNotification>()

    fun addNotification(notification: WatchNotification) {
        notifications.add(notification)
        if (!notification.isRead) {
            unreadNotifications.add(notification)
        }
    }

    fun getAllNotifications(): List<WatchNotification> = notifications.toList()

    fun getUnreadNotifications(): List<WatchNotification> = unreadNotifications.toList()

    fun getNotificationCount(): Int = notifications.size

    fun getUnreadCount(): Int = unreadNotifications.size

    fun markAsRead(notificationId: String) {
        val notification = notifications.find { it.id == notificationId }
        if (notification != null) {
            notifications.find { it.id == notificationId }?.let {
                it.isRead = true
            }
            unreadNotifications.removeIf { it.id == notificationId }
        }
    }

    fun dismissNotification(notificationId: String) {
        notifications.removeIf { it.id == notificationId }
        unreadNotifications.removeIf { it.id == notificationId }
    }

    fun dismissAllNotifications() {
        notifications.clear()
        unreadNotifications.clear()
    }

    fun groupByApp(): Map<String, List<WatchNotification>> {
        return notifications.groupBy { it.appName }
    }

    fun groupByConversation(): Map<String, List<WatchNotification>> {
        // Group by conversation thread (using title prefix or similar)
        return notifications.groupBy { extractConversationId(it) }
    }

    fun groupByTimeWindow(windowMs: Long = 3600000L): List<List<WatchNotification>> {
        // Group notifications within time windows
        val sorted = notifications.sortedBy { it.timestamp }
        val groups = mutableListOf<List<WatchNotification>>()
        var currentGroup = mutableListOf<WatchNotification>()
        var windowStart: Long? = null

        for (notification in sorted) {
            if (windowStart == null) {
                windowStart = notification.timestamp
            } else if (notification.timestamp - windowStart!! > windowMs) {
                groups.add(currentGroup.toList())
                currentGroup = mutableListOf()
                windowStart = notification.timestamp
            }
            currentGroup.add(notification)
        }

        if (currentGroup.isNotEmpty()) {
            groups.add(currentGroup)
        }

        return groups
    }

    private fun extractConversationId(notification: WatchNotification): String {
        // Simple conversation ID extraction from title
        return notification.title.take(20)
    }

    fun getNotificationsByPriority(priority: Priority): List<WatchNotification> {
        return notifications.filter { it.priority == priority }
    }

    fun getNotificationsByApp(appName: String): List<WatchNotification> {
        return notifications.filter { it.appName == appName }
    }

    fun markAllAsRead() {
        notifications.forEach { it.isRead = true }
        unreadNotifications.clear()
    }
}

// ==================== NOTIFICATION SUMMARIZER ====================

class NotificationSummarizer {

    fun summarizeSingle(notification: WatchNotification): String {
        // Truncate and format for watch display
        val title = truncate(notification.title, 30)
        val content = truncate(notification.content, 50)
        return "$title: $content"
    }

    fun summarizeBatch(notifications: List<WatchNotification>): String {
        if (notifications.isEmpty()) return "No notifications"

        val groupedByApp = notifications.groupBy { it.appName }
        val summaryParts = groupedByApp.entries.map { (app, appNotifications) ->
            val count = appNotifications.size
            val type = if (count == 1) "message" else "messages"
            "$count $type from $app"
        }

        return summaryParts.joinToString(", ")
    }

    fun summarizeByPriority(notifications: List<WatchNotification>): Map<Priority, String> {
        val priorityGroups = notifications.groupBy { it.priority }
        return priorityGroups.mapValues { (priority, prioNotifications) ->
            summarizeBatch(prioNotifications)
        }
    }

    fun generateSummaryText(notifications: List<WatchNotification>): String {
        if (notifications.isEmpty()) return "No new notifications"

        val unreadCount = notifications.count { !it.isRead }
        val total = notifications.size

        if (unreadCount == total) {
            return "You have $unreadCount new notification${if (unreadCount > 1) "s" else ""}"
        } else {
            return "$unreadCount of $total notifications are new"
        }
    }

    private fun truncate(text: String, maxLength: Int): String {
        if (text.length <= maxLength) return text
        return text.take(maxLength - 3) + "..."
    }
}

// ==================== QUICK REPLY GENERATOR ====================

class QuickReplyGenerator {

    // Smart defaults
    private val DEFAULT_REPLIES = listOf("OK", "Thanks", "On my way", "Got it", "Will do")

    fun generateReplies(notification: WatchNotification): List<String> {
        val contextReplies = generateContextualReplies(notification)
        val allReplies = (contextReplies + DEFAULT_REPLIES).distinct()
        return allReplies.take(3)
    }

    fun generateRepliesForBatch(notifications: List<WatchNotification>): List<String> {
        if (notifications.isEmpty()) return DEFAULT_REPLIES.take(3)

        // Generate replies for the most urgent notification
        val sorted = notifications.sortedByDescending { getPriorityScore(it) }
        return generateReplies(sorted.first())
    }

    fun generateRepliesForApp(appName: String, notifications: List<WatchNotification>): List<String> {
        val appNotifications = notifications.filter { it.appName == appName }
        if (appNotifications.isEmpty()) return DEFAULT_REPLIES.take(3)

        val sorted = appNotifications.sortedByDescending { getPriorityScore(it) }
        return generateReplies(sorted.first())
    }

    private fun generateContextualReplies(notification: WatchNotification): List<String> {
        val content = notification.content.lowercase()
        val title = notification.title.lowercase()
        val replies = mutableListOf<String>()

        // Time-related queries
        if (content.contains("time") || title.contains("time")) {
            replies.add("I'll check")
            replies.add("Let me look")
        }

        // Meeting/Calendar related
        if (content.contains("meeting") || content.contains("calendar") || title.contains("meeting")) {
            replies.add("Can I reschedule?")
            replies.add("I'll be there")
        }

        // Urgent/Important
        if (notification.priority == Priority.URGENT || notification.priority == Priority.HIGH) {
            replies.add("I'm on it")
            replies.add("Will handle immediately")
        }

        // Message/Chat related
        if (content.contains("hey") || content.contains("hi") || content.contains("hello")) {
            replies.add("Hey there!")
            replies.add("What's up?")
        }

        // Question-related
        if (content.contains("?") || content.contains("what") || content.contains("when") || content.contains("where")) {
            replies.add("Let me get back to you")
            replies.add("I'll look into it")
        }

        // Work-related
        if (content.contains("work") || content.contains("project") || content.contains("task")) {
            replies.add("I'll review")
            replies.add("Thanks for the update")
        }

        return replies
    }

    private fun getPriorityScore(notification: WatchNotification): Int {
        return when (notification.priority) {
            Priority.URGENT -> 4
            Priority.HIGH -> 3
            Priority.MEDIUM -> 2
            Priority.LOW -> 1
        }
    }
}

// ==================== PRIORITY SCORER ====================

class PriorityScorer {

    private val URGENT_KEYWORDS = listOf("urgent", "emergency", "asap", "immediately", "critical", "important")
    private val HIGH_KEYWORDS = listOf("meeting", "deadline", "call", "call me", "call now")
    private val TIME_SENSITIVE_KEYWORDS = listOf("today", "now", "tonight", "this week", "by end")

    fun calculatePriority(content: String, title: String, sender: String): Priority {
        val fullText = "$title $content".lowercase()

        // Check for urgent keywords
        if (URGENT_KEYWORDS.any { fullText.contains(it) }) {
            return Priority.URGENT
        }

        // Check for high priority keywords
        if (HIGH_KEYWORDS.any { fullText.contains(it) }) {
            return Priority.HIGH
        }

        // Check for time sensitivity
        if (TIME_SENSITIVE_KEYWORDS.any { fullText.contains(it) }) {
            return Priority.HIGH
        }

        // Check sender importance (simplified - in real app would check contacts)
        if (sender.lowercase().contains("boss") || sender.lowercase().contains("manager")) {
            return Priority.HIGH
        }

        // Default to medium
        return Priority.MEDIUM
    }

    fun scoreNotification(notification: WatchNotification): Int {
        var score = when (notification.priority) {
            Priority.URGENT -> 100
            Priority.HIGH -> 75
            Priority.MEDIUM -> 50
            Priority.LOW -> 25
        }

        // Boost for unread
        if (!notification.isRead) {
            score += 10
        }

        // Boost for recent
        val hoursSince = (System.currentTimeMillis() - notification.timestamp) / 3600000
        if (hoursSince < 1) {
            score += 20
        } else if (hoursSince < 24) {
            score += 10
        }

        return score
    }

    fun sortNotificationsByPriority(notifications: List<WatchNotification>): List<WatchNotification> {
        return notifications.sortedByDescending { scoreNotification(it) }
    }
}

// ========================= TESTS =========================

class NotificationManagerTest {

    private lateinit var manager: NotificationManager
    private lateinit var testNotification: WatchNotification

    @Before
    fun setUp() {
        manager = NotificationManager()
        testNotification = WatchNotification(
            id = "1",
            appName = "Messages",
            title = "Hello",
            content = "Hi there!",
            timestamp = System.currentTimeMillis(),
            priority = Priority.MEDIUM
        )
    }

    @Test
    fun `initial state is empty`() {
        assertEquals(0, manager.getNotificationCount())
        assertEquals(0, manager.getUnreadCount())
        assertTrue(manager.getAllNotifications().isEmpty())
        assertTrue(manager.getUnreadNotifications().isEmpty())
    }

    @Test
    fun `add notification increments count`() {
        manager.addNotification(testNotification)
        assertEquals(1, manager.getNotificationCount())
        assertEquals(1, manager.getUnreadCount())
    }

    @Test
    fun `mark as read updates unread count`() {
        manager.addNotification(testNotification)
        assertEquals(1, manager.getUnreadCount())
        manager.markAsRead("1")
        assertEquals(0, manager.getUnreadCount())
        assertEquals(1, manager.getNotificationCount())
    }

    @Test
    fun `dismiss notification removes it`() {
        manager.addNotification(testNotification)
        assertEquals(1, manager.getNotificationCount())
        manager.dismissNotification("1")
        assertEquals(0, manager.getNotificationCount())
        assertEquals(0, manager.getUnreadCount())
    }

    @Test
    fun `dismiss all notifications clears everything`() {
        manager.addNotification(testNotification)
        val notification2 = testNotification.copy(id = "2", appName = "Email")
        manager.addNotification(notification2)
        assertEquals(2, manager.getNotificationCount())
        manager.dismissAllNotifications()
        assertEquals(0, manager.getNotificationCount())
    }

    @Test
    fun `group by app separates notifications`() {
        val emailNotification = testNotification.copy(id = "2", appName = "Email")
        manager.addNotification(testNotification)
        manager.addNotification(emailNotification)

        val grouped = manager.groupByApp()
        assertEquals(2, grouped.size)
        assertTrue(grouped.containsKey("Messages"))
        assertTrue(grouped.containsKey("Email"))
        assertEquals(1, grouped["Messages"]?.size)
        assertEquals(1, grouped["Email"]?.size)
    }

    @Test
    fun `group by conversation works`() {
        val notification2 = testNotification.copy(id = "2", title = "Hello again")
        manager.addNotification(testNotification)
        manager.addNotification(notification2)

        val grouped = manager.groupByConversation()
        assertTrue(grouped.size >= 1)
    }

    @Test
    fun `group by time window creates groups`() {
        val notification2 = testNotification.copy(
            id = "2",
            timestamp = System.currentTimeMillis() + 7200000L // 2 hours later
        )
        manager.addNotification(testNotification)
        manager.addNotification(notification2)

        val groups = manager.groupByTimeWindow(3600000L) // 1 hour window
        assertEquals(2, groups.size)
    }

    @Test
    fun `get notifications by priority filters correctly`() {
        val lowNotification = testNotification.copy(id = "2", priority = Priority.LOW)
        manager.addNotification(testNotification)
        manager.addNotification(lowNotification)

        val highPriority = manager.getNotificationsByPriority(Priority.MEDIUM)
        val lowPriority = manager.getNotificationsByPriority(Priority.LOW)

        assertEquals(1, highPriority.size)
        assertEquals(1, lowPriority.size)
    }

    @Test
    fun `get notifications by app filters correctly`() {
        val emailNotification = testNotification.copy(id = "2", appName = "Email")
        manager.addNotification(testNotification)
        manager.addNotification(emailNotification)

        val messages = manager.getNotificationsByApp("Messages")
        val emails = manager.getNotificationsByApp("Email")

        assertEquals(1, messages.size)
        assertEquals(1, emails.size)
    }

    @Test
    fun `mark all as read clears unread`() {
        manager.addNotification(testNotification)
        val notification2 = testNotification.copy(id = "2")
        manager.addNotification(notification2)
        assertEquals(2, manager.getUnreadCount())
        manager.markAllAsRead()
        assertEquals(0, manager.getUnreadCount())
    }

    @Test
    fun `mark as read on non-existent notification does nothing`() {
        manager.addNotification(testNotification)
        manager.markAsRead("nonexistent")
        assertEquals(1, manager.getNotificationCount())
        assertEquals(1, manager.getUnreadCount())
    }

    @Test
    fun `dismiss on non-existent notification does nothing`() {
        manager.addNotification(testNotification)
        manager.dismissNotification("nonexistent")
        assertEquals(1, manager.getNotificationCount())
    }
}

class WatchNotificationTest {

    @Test
    fun `default isRead is false`() {
        val notification = WatchNotification(
            id = "1",
            appName = "Test",
            title = "Test",
            content = "Test",
            timestamp = System.currentTimeMillis(),
            priority = Priority.MEDIUM
        )
        assertFalse(notification.isRead)
    }

    @Test
    fun `can create with isRead true`() {
        val notification = WatchNotification(
            id = "1",
            appName = "Test",
            title = "Test",
            content = "Test",
            timestamp = System.currentTimeMillis(),
            priority = Priority.MEDIUM,
            isRead = true
        )
        assertTrue(notification.isRead)
    }

    @Test
    fun `notification properties are accessible`() {
        val notification = WatchNotification(
            id = "test-id",
            appName = "TestApp",
            title = "Test Title",
            content = "Test Content",
            timestamp = 12345L,
            priority = Priority.HIGH
        )
        assertEquals("test-id", notification.id)
        assertEquals("TestApp", notification.appName)
        assertEquals("Test Title", notification.title)
        assertEquals("Test Content", notification.content)
        assertEquals(12345L, notification.timestamp)
        assertEquals(Priority.HIGH, notification.priority)
    }
}

class NotificationSummarizerTest {

    private lateinit var summarizer: NotificationSummarizer
    private lateinit var notification1: WatchNotification
    private lateinit var notification2: WatchNotification
    private lateinit var notification3: WatchNotification

    @Before
    fun setUp() {
        summarizer = NotificationSummarizer()
        val baseTime = System.currentTimeMillis()
        notification1 = WatchNotification(
            id = "1",
            appName = "Messages",
            title = "Hello",
            content = "Hi there, how are you?",
            timestamp = baseTime,
            priority = Priority.MEDIUM
        )
        notification2 = WatchNotification(
            id = "2",
            appName = "Messages",
            title = "Meeting",
            content = "Can we meet tomorrow at 3pm?",
            timestamp = baseTime + 1000,
            priority = Priority.HIGH
        )
        notification3 = WatchNotification(
            id = "3",
            appName = "Email",
            title = "Project Update",
            content = "The project is on track",
            timestamp = baseTime + 2000,
            priority = Priority.LOW
        )
    }

    @Test
    fun `summarize single notification truncates long text`() {
        val longNotification = WatchNotification(
            id = "1",
            appName = "Test",
            title = "This is a very long title that exceeds the limit",
            content = "This is a very long content that exceeds the limit significantly",
            timestamp = System.currentTimeMillis(),
            priority = Priority.MEDIUM
        )
        val summary = summarizer.summarizeSingle(longNotification)
        assertTrue(summary.length < 90) // title(30) + ": " + content(50)
    }

    @Test
    fun `summarize single notification formats correctly`() {
        val summary = summarizer.summarizeSingle(notification1)
        assertTrue(summary.contains(notification1.title))
        assertTrue(summary.contains(notification1.content))
    }

    @Test
    fun `summarize batch with empty list returns message`() {
        val summary = summarizer.summarizeBatch(emptyList())
        assertEquals("No notifications", summary)
    }

    @Test
    fun `summarize batch groups by app`() {
        val summary = summarizer.summarizeBatch(listOf(notification1, notification2, notification3))
        assertTrue(summary.contains("2 messages from Messages"))
        assertTrue(summary.contains("1 message from Email"))
    }

    @Test
    fun `summarize batch handles single notification`() {
        val summary = summarizer.summarizeBatch(listOf(notification1))
        assertTrue(summary.contains("1 message from Messages"))
    }

    @Test
    fun `summarize by priority creates separate summaries`() {
        val grouped = summarizer.summarizeByPriority(listOf(notification1, notification2, notification3))
        assertEquals(3, grouped.size)
        assertTrue(grouped.containsKey(Priority.MEDIUM))
        assertTrue(grouped.containsKey(Priority.HIGH))
        assertTrue(grouped.containsKey(Priority.LOW))
    }

    @Test
    fun `generate summary text shows unread count`() {
        val summary = summarizer.generateSummaryText(listOf(notification1, notification2))
        assertTrue(summary.contains("2 new notification"))
    }

    @Test
    fun `generateSummaryTextShowsPartialUnreadCount`() {
        val readNotification = notification1.copy(isRead = true)
        val summary = summarizer.generateSummaryText(listOf(readNotification, notification2))
        assertTrue(summary.contains("1 of 2"))
    }

    @Test
    fun `generate summary text for empty list`() {
        val summary = summarizer.generateSummaryText(emptyList())
        assertEquals("No new notifications", summary)
    }

    @Test
    fun `truncate respects max length`() {
        val notification = WatchNotification(
            id = "1",
            appName = "Test",
            title = "A".repeat(100),
            content = "B".repeat(100),
            timestamp = System.currentTimeMillis(),
            priority = Priority.MEDIUM
        )
        val summary = summarizer.summarizeSingle(notification)
        // Should be title(30) + ": " + content(50) = 83 chars max
        assertTrue(summary.length <= 83)
    }
}

class QuickReplyGeneratorTest {

    private lateinit var generator: QuickReplyGenerator
    private lateinit var testNotification: WatchNotification

    @Before
    fun setUp() {
        generator = QuickReplyGenerator()
        testNotification = WatchNotification(
            id = "1",
            appName = "Messages",
            title = "Meeting",
            content = "Can we meet tomorrow at 3pm?",
            timestamp = System.currentTimeMillis(),
            priority = Priority.HIGH
        )
    }

    @Test
    fun `generates exactly 3 replies`() {
        val replies = generator.generateReplies(testNotification)
        assertEquals(3, replies.size)
    }

    @Test
    fun `includes default replies when no context matches`() {
        val simpleNotification = WatchNotification(
            id = "1",
            appName = "Test",
            title = "Test",
            content = "Hello",
            timestamp = System.currentTimeMillis(),
            priority = Priority.LOW
        )
        val replies = generator.generateReplies(simpleNotification)
        assertTrue(replies.any { it in listOf("OK", "Thanks", "On my way", "Got it", "Will do") })
    }

    @Test
    fun `generates time-related replies for time queries`() {
        val timeNotification = testNotification.copy(
            title = "What time is it?",
            content = "What time is the meeting?"
        )
        val replies = generator.generateReplies(timeNotification)
        assertTrue(replies.any { it.contains("check") || it.contains("look") })
    }

    @Test
    fun `generates meeting-related replies for calendar messages`() {
        val meetingNotification = testNotification.copy(
            title = "Meeting",
            content = "Can we reschedule the meeting?"
        )
        val replies = generator.generateReplies(meetingNotification)
        assertTrue(replies.any { it.contains("reschedule") || it.contains("be there") })
    }

    @Test
    fun `generates urgent replies for high priority`() {
        val urgentNotification = testNotification.copy(
            priority = Priority.URGENT,
            content = "URGENT: Fix this now!"
        )
        val replies = generator.generateReplies(urgentNotification)
        assertTrue(replies.any { it.contains("on it") || it.contains("immediately") })
    }

    @Test
    fun `generatesGreetingRepliesForHelloHiMessages`() {
        val greetingNotification = testNotification.copy(
            title = "Hi",
            content = "Hey there!"
        )
        val replies = generator.generateReplies(greetingNotification)
        assertTrue(replies.any { it.contains("Hey") || it.contains("What's up") })
    }

    @Test
    fun `generatesQuestionRepliesForQueries`() {
        val queryNotification = testNotification.copy(
            title = "Question",
            content = "What is the status?"
        )
        val replies = generator.generateReplies(queryNotification)
        assertTrue(replies.any { it.contains("get back") || it.contains("look into") })
    }

    @Test
    fun `generate replies for batch uses most urgent`() {
        val lowPriority = testNotification.copy(
            id = "2",
            priority = Priority.LOW,
            content = "Low priority message"
        )
        val replies = generator.generateRepliesForBatch(listOf(lowPriority, testNotification))
        // Should use replies from the high priority notification
        assertTrue(replies.any { it.contains("reschedule") || it.contains("be there") })
    }

    @Test
    fun `generate replies for app filters by app name`() {
        val emailNotification = testNotification.copy(appName = "Email")
        val replies = generator.generateRepliesForApp("Email", listOf(emailNotification))
        assertEquals(3, replies.size)
    }

    @Test
    fun `replies are unique`() {
        val replies = generator.generateReplies(testNotification)
        assertEquals(replies.toSet().size, replies.size)
    }
}

class PriorityScorerTest {

    private lateinit var scorer: PriorityScorer

    @Before
    fun setUp() {
        scorer = PriorityScorer()
    }

    @Test
    fun `urgent keywords set priority to urgent`() {
        val priority = scorer.calculatePriority(
            "This is urgent and critical",
            "Emergency",
            "Unknown"
        )
        assertEquals(Priority.URGENT, priority)
    }

    @Test
    fun `high keywords set priority to high`() {
        val priority = scorer.calculatePriority(
            "We have a meeting at 3pm",
            "Meeting",
            "Unknown"
        )
        assertEquals(Priority.HIGH, priority)
    }

    @Test
    fun `time sensitive keywords set priority to high`() {
        val priority = scorer.calculatePriority(
            "Need this today",
            "Task",
            "Unknown"
        )
        assertEquals(Priority.HIGH, priority)
    }

    @Test
    fun `boss sender sets priority to high`() {
        val priority = scorer.calculatePriority(
            "Regular message",
            "Update",
            "Boss"
        )
        assertEquals(Priority.HIGH, priority)
    }

    @Test
    fun `noSpecialKeywordsDefaultsToMedium`() {
        val priority = scorer.calculatePriority(
            "Just a regular message",
            "Notification",
            "Unknown"
        )
        assertEquals(Priority.MEDIUM, priority)
    }

    @Test
    fun `score notification boosts unread`() {
        val unread = WatchNotification(
            id = "1",
            appName = "Test",
            title = "Test",
            content = "Test",
            timestamp = System.currentTimeMillis(),
            priority = Priority.MEDIUM,
            isRead = false
        )
        val read = unread.copy(isRead = true)

        val unreadScore = scorer.scoreNotification(unread)
        val readScore = scorer.scoreNotification(read)

        assertTrue(unreadScore > readScore)
    }

    @Test
    fun `score notification boosts recent`() {
        val recent = WatchNotification(
            id = "1",
            appName = "Test",
            title = "Test",
            content = "Test",
            timestamp = System.currentTimeMillis(),
            priority = Priority.MEDIUM,
            isRead = false
        )
        val old = WatchNotification(
            id = "2",
            appName = "Test",
            title = "Test",
            content = "Test",
            timestamp = System.currentTimeMillis() - 48 * 3600000L, // 2 days ago
            priority = Priority.MEDIUM,
            isRead = false
        )

        val recentScore = scorer.scoreNotification(recent)
        val oldScore = scorer.scoreNotification(old)

        assertTrue(recentScore > oldScore)
    }

    @Test
    fun `sort notifications by priority orders correctly`() {
        val notifications = listOf(
            WatchNotification("1", "A", "", "", System.currentTimeMillis(), Priority.LOW),
            WatchNotification("2", "B", "", "", System.currentTimeMillis(), Priority.URGENT),
            WatchNotification("3", "C", "", "", System.currentTimeMillis(), Priority.HIGH),
            WatchNotification("4", "D", "", "", System.currentTimeMillis(), Priority.MEDIUM)
        )

        val sorted = scorer.sortNotificationsByPriority(notifications)
        assertEquals(Priority.URGENT, sorted[0].priority)
        assertEquals(Priority.HIGH, sorted[1].priority)
        assertEquals(Priority.MEDIUM, sorted[2].priority)
        assertEquals(Priority.LOW, sorted[3].priority)
    }

    @Test
    fun `calculate priority is case insensitive`() {
        val priority = scorer.calculatePriority(
            "This is URGENT and important",
            "URGENT",
            "BOSS"
        )
        assertEquals(Priority.URGENT, priority)
    }
}

// ==================== INTEGRATION TESTS ====================

class NotificationSystemIntegrationTest {

    private lateinit var manager: NotificationManager
    private lateinit var summarizer: NotificationSummarizer
    private lateinit var replyGenerator: QuickReplyGenerator
    private lateinit var priorityScorer: PriorityScorer

    @Before
    fun setUp() {
        manager = NotificationManager()
        summarizer = NotificationSummarizer()
        replyGenerator = QuickReplyGenerator()
        priorityScorer = PriorityScorer()
    }

    @Test
    fun `fullWorkflowReceiveSummarizeSuggestReplies`() {
        val notifications = listOf(
            WatchNotification("1", "Messages", "Hi", "Hey there!", System.currentTimeMillis(), Priority.MEDIUM),
            WatchNotification("2", "Messages", "Meeting", "Can we meet tomorrow?", System.currentTimeMillis() + 1000, Priority.HIGH),
            WatchNotification("3", "Email", "Update", "Project is on track", System.currentTimeMillis() + 2000, Priority.LOW)
        )

        notifications.forEach { manager.addNotification(it) }

        val batchSummary = summarizer.summarizeBatch(notifications)
        val quickReplies = replyGenerator.generateRepliesForBatch(notifications)
        val sorted = priorityScorer.sortNotificationsByPriority(notifications)

        assertTrue(batchSummary.contains("Messages"))
        assertTrue(batchSummary.contains("Email"))
        assertEquals(3, quickReplies.size)
        assertEquals(Priority.HIGH, sorted[0].priority)
    }

    @Test
    fun `groupingAndFilteringWorkflow`() {
        val notifications = listOf(
            WatchNotification("1", "Messages", "Hi", "Hey", System.currentTimeMillis(), Priority.MEDIUM),
            WatchNotification("2", "Messages", "Meeting", "Tomorrow", System.currentTimeMillis() + 1000, Priority.HIGH),
            WatchNotification("3", "Email", "Update", "Project", System.currentTimeMillis() + 2000, Priority.LOW),
            WatchNotification("4", "Email", "Meeting", "Call", System.currentTimeMillis() + 3000, Priority.HIGH)
        )

        notifications.forEach { manager.addNotification(it) }

        val byApp = manager.groupByApp()
        val byPriority = manager.getNotificationsByPriority(Priority.HIGH)

        assertEquals(2, byApp["Messages"]?.size)
        assertEquals(2, byApp["Email"]?.size)
        assertEquals(2, byPriority.size)
    }

    @Test
    fun `notificationLifecycleManagement`() {
        val notification = WatchNotification(
            "1", "Test", "Title", "Content", System.currentTimeMillis(), Priority.MEDIUM
        )
        manager.addNotification(notification)

        assertEquals(1, manager.getNotificationCount())
        assertEquals(1, manager.getUnreadCount())

        manager.markAsRead("1")
        assertEquals(0, manager.getUnreadCount())

        manager.dismissNotification("1")
        assertEquals(0, manager.getNotificationCount())
    }

    @Test
    fun `batchOperationsWorkCorrectly`() {
        val notifications = (1..5).map { i ->
            WatchNotification(
                "$i", "Test", "Title $i", "Content $i", System.currentTimeMillis(), Priority.MEDIUM
            )
        }

        notifications.forEach { manager.addNotification(it) }

        assertEquals(5, manager.getNotificationCount())
        manager.markAllAsRead()
        assertEquals(0, manager.getUnreadCount())

        manager.dismissAllNotifications()
        assertEquals(0, manager.getNotificationCount())
    }

    @Test
    fun `timeBasedGroupingWorks`() {
        val baseTime = System.currentTimeMillis()
        val notifications = listOf(
            WatchNotification("1", "A", "", "", baseTime, Priority.MEDIUM),
            WatchNotification("2", "B", "", "", baseTime + 1800000L, Priority.MEDIUM), // 30 min later
            WatchNotification("3", "C", "", "", baseTime + 3600000L, Priority.MEDIUM), // 1 hour later
            WatchNotification("4", "D", "", "", baseTime + 5400000L, Priority.MEDIUM)  // 1.5 hours later
        )

        notifications.forEach { manager.addNotification(it) }

        val groups = manager.groupByTimeWindow(3600000L) // 1 hour windows
        assertTrue(groups.size >= 2) // Should have at least 2 groups
    }
}
