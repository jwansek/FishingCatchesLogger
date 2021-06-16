public class CurrentUser {
    private static LocalDatabase.FishingUser currentUser;

    public static LocalDatabase.FishingUser getUser() {
        return currentUser;
    }

    public static void startSession(LocalDatabase.FishingUser user) {
        currentUser = user;
    }

    public static void endSession() {
        currentUser = null;
    }
}
