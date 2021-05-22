package mflix.api.daos;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoWriteException;
import com.mongodb.WriteConcern;
import com.mongodb.WriteConcernException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import mflix.api.models.Session;
import mflix.api.models.User;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Configuration
public class UserDao extends AbstractMFlixDao {

    private final MongoCollection<User> usersCollection;
    //TODO> Ticket: User Management - do the necessary changes so that the sessions collection
    //returns a Session object
    private final MongoCollection<Session> sessionsCollection;

    private final Logger log;

    @Autowired
    public UserDao(
            MongoClient mongoClient, @Value("${spring.mongodb.database}") String databaseName) {
        super(mongoClient, databaseName);
        CodecRegistry pojoCodecRegistry =
                fromRegistries(
                        MongoClientSettings.getDefaultCodecRegistry(),
                        fromProviders(PojoCodecProvider.builder().automatic(true).build()));

        usersCollection = db.getCollection("users", User.class).withCodecRegistry(pojoCodecRegistry);
        log = LoggerFactory.getLogger(this.getClass());
        //TODO> Ticket: User Management - implement the necessary changes so that the sessions
        // collection returns a Session objects instead of Document objects.
        sessionsCollection = db.getCollection( "sessions", Session.class).withCodecRegistry(pojoCodecRegistry);;
    }

    /**
     * Inserts the `user` object in the `users` collection.
     *
     * @param user - User object to be added
     * @return True if successful, throw IncorrectDaoOperation otherwise
     */
    public boolean addUser(User user) throws IncorrectDaoOperation {
        //TODO > Ticket: Handling Errors - make sure to only add new users and not users that already exist.
        try {
            //TODO > Ticket: Durable Writes -  you might want to use a more durable write concern here!
            this.usersCollection
                    .withWriteConcern(WriteConcern.MAJORITY)
                    .insertOne(user);
            return true;
        }
        catch (MongoWriteException | WriteConcernException exc) {
            throw new IncorrectDaoOperation(exc.getMessage(), exc);
        }
    }

    /**
     * Creates session using userId and jwt token.
     *
     * @param userId - user string identifier
     * @param jwt    - jwt string token
     * @return true if successful
     */
    public boolean createUserSession(String userId, String jwt) {
        //TODO > Ticket: Handling Errors - implement a safeguard against
        // creating a session with the same jwt token.
        try
        {
            //TODO> Ticket: User Management - implement the method that allows session information to be
            // stored in it's designated collection.
            Bson updateFilter = new Document("user_id", userId);
            Bson setUpdate = Updates.set("jwt", jwt);
            UpdateOptions options = new UpdateOptions().upsert(true);
            sessionsCollection.updateOne(updateFilter, setUpdate, options);
            return true; // changed to true
        }
        catch (MongoWriteException | WriteConcernException exc) {
            return false;
        }
    }

    /**
     * Returns the User object matching the an email string value.
     *
     * @param email - email string to be matched.
     * @return User object or null.
     */
    public User getUser(String email) {
        User user = null;
        //TODO> Ticket: User Management - implement the query that returns the first User object.
        user = this.usersCollection.find(Filters.eq("email", email)).limit(1).first();
        return user;
    }

    /**
     * Given the userId, returns a Session object.
     *
     * @param userId - user string identifier.
     * @return Session object or null.
     */
    public Session getUserSession(String userId) {
        //TODO> Ticket: User Management - implement the method that returns Sessions for a given
        // userId
        Session session = this.sessionsCollection.find(Filters.eq("user_id", userId)).limit(1).first();
        return session;
    }

    public boolean deleteUserSessions(String userId) {
        //TODO> Ticket: User Management - implement the delete user sessions method
        DeleteResult deleteResult = this.sessionsCollection.deleteOne(Filters.eq("user_id", userId));
        return deleteResult.wasAcknowledged() && deleteResult.getDeletedCount() > 0;
    }

    /**
     * Removes the user document that match the provided email.
     *
     * @param email - of the user to be deleted.
     * @return true if user successfully removed
     */
    public boolean deleteUser(String email) {
        //TODO > Ticket: Handling Errors - make this method more robust by
        // handling potential exceptions.

        try {
            // remove user sessions
            //TODO> Ticket: User Management - implement the delete user method
            this.deleteUserSessions(email);
            DeleteResult deleteResult = this.usersCollection.deleteMany(Filters.eq("email", email));
            return deleteResult.wasAcknowledged() && deleteResult.getDeletedCount() > 0;
        }
        catch (WriteConcernException | MongoWriteException exc) {
            return false;
        }
    }

    /**
     * Updates the preferences of an user identified by `email` parameter.
     *
     * @param email           - user to be updated email
     * @param userPreferences - set of preferences that should be stored and replace the existing
     *                        ones. Cannot be set to null value
     * @return User object that just been updated.
     */
    public boolean updateUserPreferences(String email, Map<String, ?> userPreferences) {
        //TODO > Ticket: Handling Errors - make this method more robust by
        // handling potential exceptions when updating an entry.
        try {
            //TODO> Ticket: User Preferences - implement the method that allows for user preferences to
            // be updated.
            if(userPreferences != null) {
                Map<String, Object> notNullPreferences = new HashMap<>();
                for (Map.Entry<String, ?> entry: userPreferences.entrySet()) {
                    Object value = entry.getValue();
                    if(entry.getKey() != null &&  value != null) {
                        notNullPreferences.put(entry.getKey(), value);
                    }
                }

                Bson updateFilter = new Document("email", email);
                Bson setUpdate = Updates.set("preferences", notNullPreferences);
                UpdateResult result = this.usersCollection.updateOne(updateFilter, setUpdate);
                return result.wasAcknowledged() && result.getModifiedCount() > 0;
            }
            else {
                throw new IncorrectDaoOperation("updateUserPreferences should not be null");
            }
        }
        catch (MongoWriteException | WriteConcernException exc) {
            throw  new IncorrectDaoOperation(exc.getMessage(), exc);
        }
    }
}
