export function formatLastSeen(lastSeenTimestamp) {
    if (!lastSeenTimestamp) return 'Last seen recently';
    
    const lastSeen = new Date(lastSeenTimestamp);
    const now = new Date();
    
    // Format time part consistently
    const timeFormatter = new Intl.DateTimeFormat('en', { 
        hour: 'numeric', 
        minute: 'numeric',
        hour12: true 
    });
    const timeString = timeFormatter.format(lastSeen);
    
    // Check if it's today, yesterday, or earlier
    const isToday = lastSeen.toDateString() === now.toDateString();
    
    // Yesterday check - compare date strings after subtracting 1 day from now
    const yesterday = new Date(now);
    yesterday.setDate(yesterday.getDate() - 1);
    const isYesterday = lastSeen.toDateString() === yesterday.toDateString();
    
    // Calculate time difference in seconds
    const diffSeconds = Math.floor((now - lastSeen) / 1000);
    
    if (diffSeconds < 60) {
        return 'Last seen just now';
    } else if (isToday) {
        return `Last seen today at ${timeString}`;
    } else if (isYesterday) {
        return `Last seen yesterday at ${timeString}`;
    } else if (diffSeconds < 604800) { // Less than a week
        // Get day name
        const dayFormatter = new Intl.DateTimeFormat('en', { weekday: 'long' });
        const dayName = dayFormatter.format(lastSeen);
        return `Last seen on ${dayName} at ${timeString}`;
    } else {
        // Format full date for older timestamps
        const dateFormatter = new Intl.DateTimeFormat('en', { 
            month: 'short', 
            day: 'numeric',
            year: lastSeen.getFullYear() !== now.getFullYear() ? 'numeric' : undefined
        });
        return `Last seen on ${dateFormatter.format(lastSeen)} at ${timeString}`;
    }
}