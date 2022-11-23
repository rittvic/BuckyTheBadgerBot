package buckythebadgerbot.utility.enums;

/**
 * Enum class to store images for every dining market
 */
public enum DiningMenuImage {
    RH("https://media.discordapp.net/attachments/1007141650281279568/1036607007706333225/unknown.png?width=676&height=676"),
    GO("https://media.discordapp.net/attachments/1007141650281279568/1036607137641672854/unknown.png?width=676&height=676"),
    LO("https://media.discordapp.net/attachments/1007141650281279568/1036607389090189382/unknown.png"),
    LI("https://media.discordapp.net/attachments/1007141650281279568/1036607711472787526/unknown.png"),
    CA("https://media.discordapp.net/attachments/1007141650281279568/1036607924857995416/unknown.png?width=676&height=676"),
    FO("https://media.discordapp.net/attachments/1007141650281279568/1036608052192878635/unknown.png?width=676&height=676");

    public final String url;

    /**
     * Enum constructor
     * @param url the url of the image
     */
    DiningMenuImage(String url) {
        this.url = url;
    }
}
