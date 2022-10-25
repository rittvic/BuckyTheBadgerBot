# BuckyTheBadger Bot

A Discord bot to search for courses and professors at UW-Madison.

## How To Use The Bot

First, [click this](https://discord.com/api/oauth2/authorize?client_id=990494786123333682&permissions=139586751552&scope=bot%20applications.commands) to add the bot to your discord server.
It needs permissions to read and send messages, use slash commands, and send embed links; and most importantly, it also needs application commands scope permission to register the slash commands.

### Commands

These commands are case-insensitive.

#### Search Command

`/search <course to query>` 

`e.g., <Calculus>, <Amer Ind>, <Math 340>, <500>`

This queries through the courses and finds the best matches.  It then generates buttons for each result so you can find the course information upon clicking them.

Example -

![Example1](https://cdn.discordapp.com/attachments/1007141650281279568/1034285519481344060/unknown.png)


#### Course Command

`/course <course>`

`e.g., <CS 577>, <Biology>, <102>, <Machine Learning>`

This searches for the specified course (or the top result) and displays the following information:
- Course Description
- Cumulative GPA
- Credits
- Requisites
- Course Designation
- Repeatable For Credit
- Last Taught

Example -

![Example2](https://cdn.discordapp.com/attachments/1007141650281279568/1007153043583803422/unknown.png)

#### Professor Command

`/professor <professor>` 

`e.g., <Hobbes>, <Boya Wen>, <Vermillion>`

Searches for a professor and displays the following information:
- Department
- Average Rating
- Total Ratings
- Would Take Again
- Top Tags
- Courses Taught

Example -

![Example3](https://cdn.discordapp.com/attachments/1007141650281279568/1007153325298438236/unknown.png)

## Self Hosting

To get a local copy of the bot up and running for development and testing purposes, follow these instructions.

### Prerequisites

Install the latest version of [Java](https://www.oracle.com/java/technologies/downloads/).

### Installation

1. Clone the repo:

```
git clone https://github.com/rittvic/BuckyTheBadgerBot.git
```

2. Create a .env file in the root project folder and put in your token variables there, following the example in `.env.example`

3. You can now build and run the application using maven.

> If you want to create a .JAR file, simply run `mvn package` in your IDE and it will spit out a shaded .JAR file within the target directory of the project.

## Built With

* [Java Discord API (JDA)](https://github.com/DV8FromTheWorld/JDA) - The Java wrapper for Discord API
* [jsoup](https://github.com/jhy/jsoup/) - HTML parser library
* [JSON Processing](https://mvnrepository.com/artifact/org.glassfish/javax.json) - To parse JSON (1/2)
* [JSON Processing API](https://mvnrepository.com/artifact/javax.json/javax.json-api) - To parse JSON (2/2)
* [Dotenv](https://github.com/cdimascio/dotenv-java) - Module to load environment variables
* [Maven](https://maven.apache.org/) - Dependency management

## Where I Get The Information From
* [api.madgrades.com](https://api.madgrades.com/)
* [guide.wisc.edu](https://guide.wisc.edu)
* [ratemyprofessors.com](https://www.ratemyprofessors.com)

## Contributing

If you would like to contribute, whether it is a suggestion or bug fix, please fork the repo and create a pull request. Any contributions would be appreciated :)
1. Fork the project
2. Create your feature branch - `git checkout -b feature/YourFeature`
3. Commit your changes - `git commit -m 'Add this YourFeature'`
4. Push to the branch - `git push origin feature/YourFeature`
5. Open a pull request

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE) file for  details

<b><i> Note: This project is not affiliated with UW-Madison. </b><i>
