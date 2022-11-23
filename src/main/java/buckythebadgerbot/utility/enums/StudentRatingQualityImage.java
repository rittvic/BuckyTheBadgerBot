package buckythebadgerbot.utility.enums;

/**
 * Enum class to store an image and hex color for every student rating imageUrl
 */
public enum StudentRatingQualityImage {
    AWESOME("https://cdn.discordapp.com/attachments/1007141650281279568/1044794201583407205/image.png","#80f6c4"),
    AVERAGE("https://cdn.discordapp.com/attachments/1007141650281279568/1044794476960432178/image.png","#fff06f"),
    AWFUL("https://cdn.discordapp.com/attachments/1007141650281279568/1044794634607525958/image.png","#fe9d95");

    public final String imageUrl;
    public final String hexColor;


    /**
     * Enum constructor
     * @param imageUrl the image url of the rating imageUrl
     * @param hexColor the hex color of the rating imageUrl
     */
    StudentRatingQualityImage(String imageUrl, String hexColor) {
        this.imageUrl = imageUrl;
        this.hexColor = hexColor;
    }
}
