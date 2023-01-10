# BuckyTheBadger Bot
A Discord bot to search for courses, professors, and other information at UW-Madison.

## [Invite Bot To Your Server](https://discord.com/api/oauth2/authorize?client_id=990494786123333682&permissions=139586751552&scope=bot%20applications.commands)
It needs permissions to read and send messages, use slash commands, and send embed links; and most importantly, it also needs application commands scope permission to register slash commands.

### Commands
These commands are case-insensitive.
<br><br>
<em>Note: This page may not be up-to-date. I try my best to update it whenever new features are added.</em>

#### Search Command
`/search <course to query>` 

`e.g., <Calculus>, <Amer Ind>, <Math 340>, <500>`

Queries the courses based on the user input and returns the best matches. You can click on every result through the generated buttons.

#### Course Command
`/course <course>`

`e.g., <COMP SCI 577>, <Biology>, <102>, <Machine Learning>`
Searches for the specified course (or the top result) and displays the following information (in order):
- Course Subject, Number and Title
- Course Description
- Cumulative GPA
- Credits
- Requisites
- Course Designation
- Repeatable For Credit
- Last Taught
- Cross-listed Subjects (if any)

#### Professor Command
`/professor <professor>` 

`e.g., <Hobbes>, <Boya Wen>, <Vermillion>`

Searches for a professor and displays the following information (in order):
- Department
- Average Rating
- Total Ratings
- Would Take Again Percentage
- Top Tags
- Top Courses Taught
  You can also view student ratings for every course taught by the professor.

#### Gym Command
`/gym`
Displays live usages for every gym equipment at the Nicholas Recreation Center and the Shell.

#### Dining Hall Command
`/diningmenu <dining market> <menu type>`

`e.g., <Rheta's Market> <Breakfast>, <Gordon Avenue Market> <Lunch>, <Four Lakes Market> <Dinner>`

Displays today's dining menu (breakfast, lunch, dinner, or daily) consisting of every food station and its servings, for any one of the six dining markets.

## Development & Local Testing
To get a local copy of the bot up and running for development and testing purposes, see below:

### Prerequisites
1. Latest version of [Java (need 18.0.2+)](https://www.oracle.com/java/technologies/downloads/)
2. [Docker](https://www.docker.com/)
   * This is used to containerize both the bot and the database, which makes it easy for setup.
   Otherwise, you will have to install [PostgreSQL](https://www.postgresql.org/) on your local machine.

### Installation
1. Clone the repo:

```
git clone https://github.com/rittvic/BuckyTheBadgerBot.git
```

2. Edit the following configuration files (as necessary):
   * <b>.env</b> - Put your token variables here. Use `.env.example` as reference.
   * <b>db_init.sql</b> (optional) - This is for seeding the database. Use `db_init_example.sql` as reference.
   However, I may release my tool for this in the future.
   > This is marked as optional because the bot can still function without the database, but the commands that rely on it won't work obviously.

   * <b>docker-compose-yml</b> - Set the path to your docker image of the bot here, as well as the initialization script if you are using the database.

### With Docker
3. After making changes, build a docker image of the bot by running `docker build -t <image-name>:<tag> <path to Dockerfile>` (if in the same path, you can use `.` instead). 
Then run `docker compose up` to see the bot (and database) in action.

### Without Docker
if you are not using Docker, then after making changes, you will need to create a .JAR file of the bot, and have the database up and running on your machine.
Make sure you are using the same database configuration tokens set in `.env`. 

> To create a .JAR file, simply run `mvn package` in your IDE and it will spit out a shaded .JAR file within the target directory of the project.

### Contributing
If you would like to contribute, whether it is a suggestion or bug fix, please fork the repo and create a pull request. Any contributions would be appreciated :)
1. Fork the project
2. Create your feature branch - `git checkout -b feature/YourFeature`
3. Commit your changes - `git commit -m 'Add this YourFeature'`
4. Push to the branch - `git push origin feature/YourFeature`
5. Open a pull request

## Built With
* [Java Discord API (JDA)](https://github.com/DV8FromTheWorld/JDA) - a Java wrapper for the Discord API
* [Jackson](https://github.com/FasterXML/jackson) - High-performance Java library to parse and serialize/deserialize JSON
* [PostgreSQL](https://www.postgresql.org/) and [PostgreSQL JDBC Driver](https://jdbc.postgresql.org) - a SQL relational database management system powered by JDBC to allow Java programs to connect to the database.
* [HikariCP](https://github.com/brettwooldridge/HikariCP) - Lightweight, fast JDBC connection pooling framework
* [Docker](https://www.docker.com/) - Software containerization platform
* [Dotenv](https://github.com/cdimascio/dotenv-java) - Module to load environment variables
* [logback-classic](https://logback.qos.ch/) - Reliable, flexible logging framework
* [Maven](https://maven.apache.org/) - Dependency manager

## Where I Get The Information From
* [guide.wisc.edu](https://guide.wisc.edu)
* [ratemyprofessors.com](https://www.ratemyprofessors.com)

## License
This project is licensed under the MIT License - see the [LICENSE.md](LICENSE) file for  details

<b><i> Note: This project is not affiliated with UW-Madison. </b></i>
