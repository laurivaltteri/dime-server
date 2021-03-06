/*
  Copyright (c) 2015-2016 University of Helsinki

  Permission is hereby granted, free of charge, to any person
  obtaining a copy of this software and associated documentation files
  (the "Software"), to deal in the Software without restriction,
  including without limitation the rights to use, copy, modify, merge,
  publish, distribute, sublicense, and/or sell copies of the Software,
  and to permit persons to whom the Software is furnished to do so,
  subject to the following conditions:

  The above copyright notice and this permission notice shall be
  included in all copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
  BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
  ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
*/

package fi.hiit.dime;

import static fi.hiit.dime.search.SearchIndex.WeightType;
import static fi.hiit.dime.search.SearchIndex.weightType;

import fi.hiit.dime.authentication.CurrentUser;
import fi.hiit.dime.authentication.User;
import fi.hiit.dime.data.DiMeData;
import fi.hiit.dime.data.DiMeDataRelation;
import fi.hiit.dime.data.Event;
import fi.hiit.dime.data.EventRelation;
import fi.hiit.dime.data.InformationElement;
import fi.hiit.dime.data.InformationElementRelation;
import fi.hiit.dime.data.Profile;
import fi.hiit.dime.data.ResourcedEvent;
import fi.hiit.dime.database.EventDAO;
import fi.hiit.dime.database.InformationElementDAO;
import fi.hiit.dime.database.ProfileDAO;
import fi.hiit.dime.search.KeywordSearchQuery;
import fi.hiit.dime.search.SearchIndex.SearchQueryException;
import fi.hiit.dime.search.SearchIndex;
import fi.hiit.dime.search.SearchQuery;
import fi.hiit.dime.search.SearchResults;
import fi.hiit.dime.search.TextSearchQuery;
import fi.hiit.dime.search.WeightedKeyword;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletRequest;

/**
 * General API controller, for things that go directly under the /api
 * REST endpoint.
 *
 * @author Mats Sjöberg, mats.sjoberg@helsinki.fi
 */
@RestController
@RequestMapping("/api")
public class ApiController extends AuthorizedController {
    private static final Logger LOG =
        LoggerFactory.getLogger(ApiController.class);

    private final EventDAO eventDAO;
    private final InformationElementDAO infoElemDAO;
    private final ProfileDAO profileDAO;

    @Autowired
    private DiMeProperties dimeConfig;

    @Autowired
    SearchIndex searchIndex;

    @Autowired
    ApiController(EventDAO eventDAO,
                  InformationElementDAO infoElemDAO,
                  ProfileDAO profileDAO) {
        this.eventDAO = eventDAO;
        this.infoElemDAO = infoElemDAO;
        this.profileDAO = profileDAO;
    }

    /**
        Class for "dummy" API responses which just return a simple
        message string.
    */
    public static class ApiMessage {
        public String message;

        public ApiMessage() {}

        public ApiMessage(String message) {
            this.message = message;
        }
    }

    /**
        @api {get} /ping Ping server
        @apiName Ping
        @apiDescription A way to "ping" the dime-server to see if it's running, and there is network access.

        @apiExample {python} Example usage:
            r = requests.post(server_url + '/ping',
                              timeout=10)         # e.g. in case of network problems
            ok = r.status_code == requests.codes.ok

        @apiSuccessExample {json} Example successful response:
            HTTP/1.1 200 OK
            {
                "message": "pong"
            }
        @apiGroup Status
        @apiVersion 0.1.2
    */
    @RequestMapping("/ping")
    public ResponseEntity<ApiMessage> ping(ServletRequest req) {
        LOG.info("Received ping from " + req.getRemoteHost());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<ApiMessage>(new ApiMessage("pong"),
                                              headers, HttpStatus.OK);
    }

    /**
       Helper method to transform the search results into an
       appropriate format for returning from the API.
    */

    protected SearchResults doSearch(SearchQuery query, String className,
                                     String typeName, int limit, User user,
                                     WeightType termWeighting)
        throws IOException, SearchQueryException
    {
        if (query.isEmpty())
            return new SearchResults();

        searchIndex.updateIndex();

        SearchResults res = searchIndex.search(query, className, typeName,
                                               limit, user.getId(),
                                               termWeighting);
        searchIndex.mapToElements(res);

        LOG.info("Search query \"{}\" (limit={}) returned {} results.",
                 query, limit, res.getNumFound());
        return res;
    }

    /**
       Helper method to transform the information element search
       results into their corresponding events for returning from the
       API.
    */

    protected SearchResults doEventSearch(SearchQuery query, String className,
                                          String typeName, int limit, User user,
                                          WeightType termWeighting)
        throws IOException, SearchQueryException
    {
        if (query.isEmpty())
            return new SearchResults();

        searchIndex.updateIndex();

        SearchResults res = searchIndex.search(query, className, typeName,
                                               limit, user.getId(),
                                               termWeighting);
        searchIndex.mapToEvents(res, user);

        LOG.info("Search query \"{}\" (limit={}) returned {} results.",
                 query, limit, res.getNumFound());

        return res;
    }

    /**
       @apiDefine user User access 
       You need to be authenticated as a registered DiMe user.
    */

    /** HTTP end point for uploading a single event. 
        @api {get} /search Information element search
        @apiName SearchInformationElement
        @apiDescription Perform a text search on existing information elements in DiMe. The search is performed using Lucene in the DiMe backend.

It returns an object that contains some meta-data and in the "docs" element a list of InformationElement objects together with a score indicating the relevance of the object to the search query. The list is sorted by this score, descending.

        @apiParam {Number} query Query text to search for

        @apiParam (Options) {Number} [limit] limit the number of results
        @apiParam (Options) {Boolean} [includeTerms] set to "true" in order to include indexing terms

        @apiSuccessExample {json} Example successful response:
            HTTP/1.1 200 OK
            {
              "docs": [
                {
                  "plainTextContent": "Some text content\n",
                  "user": {
                    "id": "5524d8ede4b06e42cc0e0aca",
                    "role": "USER",
                    "username": "testuser"
                  },
                  "uri": "file:///home/testuser/some_file.txt",
                  "type": "http://www.semanticdesktop.org/ontologies/2007/03/22/nfo#TextDocument",
                  "mimeType": "text/plain",
                  "timeCreated": 1430142736819,
                  "id": "d8b5b874e4bae5a6f6260e1042281e91c69d305e",
                  "timeModified": 1430142736819,
                  "score": 0.75627613,
                  "isStoredAs": "http://www.semanticdesktop.org/ontologies/nfo#FileDataObject"
                },
                {
                  "plainTextContent": "Some other text content",
                  "user": {
                    "id": "5524d8ede4b06e42cc0e0aca",
                    "role": "USER",
                    "username": "testuser"
                  },
                  "uri": "file:///home/testuser/another_file.txt",
                  "type": "http://www.semanticdesktop.org/ontologies/2007/03/22/nfo#TextDocument",
                  "mimeType": "text/plain",
                  "timeCreated": 1430142737246,
                  "id": "99db4832be27cff6b08a1f91afbf0401cad49d15",
                  "timeModified": 1430142737246,
                  "score": 0.75342464,
                  "isStoredAs": "http://www.semanticdesktop.org/ontologies/nfo#FileDataObject"
                }
              ],
              "numFound": 2
            }

        @apiErrorExample {json} Example error response for erroneous Lucene query:
            HTTP/1.1 400 OK
            {
              "docs": [],
              "message": "Syntax Error, cannot parse a::  ",
              "numFound": 0
            }

        @apiPermission user
        @apiGroup Search
        @apiVersion 0.1.2
    */
    @RequestMapping(value="/search", method = RequestMethod.GET)
    public ResponseEntity<SearchResults>
        search(Authentication auth,
               @RequestParam String query,
               @RequestParam(value="@type", required=false) String className,
               @RequestParam(value="type", required=false) String typeName,
               @RequestParam(value="includeTerms", required=false, 
                             defaultValue="") String includeTerms,
               @RequestParam(defaultValue="-1") int limit)
    {
        User user = getUser(auth);

        try {
            TextSearchQuery textQuery = new TextSearchQuery(query);
            SearchResults results = doSearch(textQuery, className, typeName,
                                             limit, user, 
                                             weightType(includeTerms));

            return new ResponseEntity<SearchResults>(results, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<SearchResults>
                (new SearchResults(e.getMessage()),
                 HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (SearchQueryException e) {
            return new ResponseEntity<SearchResults>
                (new SearchResults(e.getMessage()),
                 HttpStatus.BAD_REQUEST);
        }
    }

    /**
        @api {get} /eventsearch Event search
        @apiName SearchEvent
        @apiDescription Perform a text search on existing events in
        DiMe. Most events do not contain any text content by
        themselves, and for these the search is actually performed
        against the information element objects they are linked to. For
        example if a document matches the search, all the events which
        refer to the object are returned. The search is performed
        using Lucene in the DiMe backend.

The end point returns a JSON list of event objects with their linked
        information element object included. Note: since the
        information elements may be repeated several times in the
        results and their text content very long, their
        plainTextContent field has been removed. (It can be fetched
        separately by id.)

The return format is the same as for the <a href="#api-Search-SearchInformationElement">information element search</a>.

        @apiParam {Number} query Query text to search for

        @apiParam (Options) {Number} [limit] limit the number of results
        @apiParam (Options) {Boolean} [includeTerms] set to "true" in order to include indexing terms

        @apiPermission user
        @apiGroup Search
        @apiVersion 0.1.2
    */
    @RequestMapping(value="/eventsearch", method = RequestMethod.GET)
    public ResponseEntity<SearchResults>
        eventSearch(Authentication auth,
                    @RequestParam String query,
                    @RequestParam(value="@type", required=false) String className,
                    @RequestParam(value="type", required=false) String typeName,
                    @RequestParam(value="includeTerms", required=false,
                                  defaultValue="") String includeTerms,
                    @RequestParam(defaultValue="-1") int limit) {
        User user = getUser(auth);

        try {
            TextSearchQuery textQuery = new TextSearchQuery(query);
            SearchResults results = doEventSearch(textQuery, className,
                                                  typeName, limit, user,
                                                  weightType(includeTerms));

            return new ResponseEntity<SearchResults>(results, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<SearchResults>
                (new SearchResults(e.getMessage()),
                 HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (SearchQueryException e) {
            return new ResponseEntity<SearchResults>
                (new SearchResults(e.getMessage()),
                 HttpStatus.BAD_REQUEST);
        }
    }

    /**
        @api {post} /keywordsearch 
        @apiName KeywordSearch
        @apiDescription Perform an information element search based on
        the POSTed weighted keywords.

        @apiPermission user
        @apiGroup Search
        @apiVersion 0.1.2
    */
    @RequestMapping(value="/keywordsearch", method = RequestMethod.POST)
    public ResponseEntity<SearchResults>
        search(Authentication auth, @RequestBody WeightedKeyword[] input) 
    {
        User user = getUser(auth);

        try {
            KeywordSearchQuery query = new KeywordSearchQuery(input);
            SearchResults results = doSearch(query, null, null,  -1, user, 
                                             WeightType.Tf);
            return new ResponseEntity<SearchResults>(results, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<SearchResults>
                (new SearchResults(e.getMessage()),
                 HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (SearchQueryException e) {
            return new ResponseEntity<SearchResults>
                (new SearchResults(e.getMessage()),
                 HttpStatus.BAD_REQUEST);
        }
    }

    /**
        @api {post} /eventkeywordsearch 
        @apiName EventKeywordSearch
        @apiDescription Perform an event search based on the POSTed weighted keywords.

        @apiPermission user
        @apiGroup Search
        @apiVersion 0.1.2
    */
    @RequestMapping(value="/eventkeywordsearch", method = RequestMethod.POST)
    public ResponseEntity<SearchResults>
        eventSearch(Authentication auth, @RequestBody WeightedKeyword[] input) 
    {
        User user = getUser(auth);

        try {
            KeywordSearchQuery query = new KeywordSearchQuery(input);
            SearchResults results = doEventSearch(query, null, null, -1, user, 
                                                  WeightType.Tf);

            return new ResponseEntity<SearchResults>(results, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<SearchResults>
                (new SearchResults(e.getMessage()),
                 HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (SearchQueryException e) {
            return new ResponseEntity<SearchResults>
                (new SearchResults(e.getMessage()),
                 HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Helper method to save a profile.
     *
     * @param input Profile to store
     * @param user current authenticated user
     * @return The profile as stored
     */
    @Transactional
    private Profile storeProfile(Profile profile, User user) {
        profile.user = user;
        profileDAO.save(profile);

        return profile;
    }

    /**
     * Helper method to update profile.
     *
     * @param profileId Id of the profile to update
     * @param relation The relation to add to the profile
     * @param validated True if the relation is a "validation" relation, otherwise it is "suggested"
     * @param user current authenticated user
     */
    @Transactional
    private Profile updateProfile(Long profileId, DiMeDataRelation relation, boolean validated, 
                                  User user) 
        throws NotFoundException, BadRequestException
    {
        try {
            Profile profile = profileDAO.findById(profileId, user);

            if (profile == null || !profile.user.getId().equals(user.getId()))
                throw new NotFoundException("Profile not found");
 
            if (relation instanceof EventRelation) {
                EventRelation eventRelation = (EventRelation)relation;
                Event event = eventDAO.findById(eventRelation.event.getId());
                
                if (event == null || !event.user.getId().equals(user.getId()))
                    throw new NotFoundException("Event not found");
 
                eventRelation.event = event;

                if (validated) 
                    profile.validateEvent(eventRelation);
                else
                    profile.addEvent(eventRelation);
            } else if (relation instanceof InformationElementRelation) {
                InformationElementRelation elemRelation = (InformationElementRelation)relation;
                InformationElement elem = 
                    infoElemDAO.findById(elemRelation.informationElement.getId());
                
                if (elem == null || !elem.user.getId().equals(user.getId()))
                    throw new NotFoundException("InformationElement not found");

                elemRelation.informationElement = elem;

                if (validated) 
                    profile.validateInformationElement(elemRelation);
                else
                    profile.addInformationElement(elemRelation);
            }                
            return storeProfile(profile, user);
        } catch (IllegalArgumentException | InvalidDataAccessApiUsageException e) {
            throw new BadRequestException("Invalid argument: " + relation);
        }
    }

    /** HTTP end point for creating a new profile. 
        @api {post} /profile Create a new profile
        @apiName PostProfile
        @apiDescription Create a new profile.
        
        @apiExample {json} Example of JSON to upload
            {
              name: "Kai's formula profile",
              search_keywords: ["x”, "y" ],
              tags: ["tag1", "tag2"],   
            }

        @apiPermission user
        @apiGroup Profiles
        @apiVersion 0.1.2
    */
    @RequestMapping(value="/profile", method = RequestMethod.POST)
    public ResponseEntity<Profile> profile(Authentication auth, @RequestBody Profile input)
        throws NotFoundException, BadRequestException
    {
        User user = getUser(auth);

        // FIXME: should be able to upload relations directly
        input.validatedEvents.clear();
        input.suggestedEvents.clear();
        input.validatedInformationElements.clear();
        input.suggestedInformationElements.clear();
        
        input = storeProfile(input, user);

        return new ResponseEntity<Profile>(input, HttpStatus.OK);
    }   

    /** HTTP end point for accessing a given profile. 
        @api {get} /profile/:id Access profile
        @apiName GetProfile
        @apiParam {Number} id Profile's unique ID

        @apiPermission user
        @apiGroup Profiles
        @apiVersion 0.1.2
     */
    @RequestMapping(value="/profile/{id}", method = RequestMethod.GET)
    public ResponseEntity<Profile>
        profile(Authentication auth, @PathVariable Long id) 
        throws NotFoundException
    {
        User user = getUser(auth);

        Profile profile = profileDAO.findById(id, user);

        if (profile == null || !profile.user.getId().equals(user.getId()))
            throw new NotFoundException("Profile not found");

        return new ResponseEntity<Profile>(profile, HttpStatus.OK);
    }   

    /** HTTP end point for deleting a profile.         
        
        @api {delete} /profile/:id Delete profile
        @apiName DeleteProfile
        @apiParam {Number} id Profile's unique ID
        @apiDescription On success, the response will be an empty HTTP 204.

        @apiPermission user
        @apiGroup Profiles
        @apiVersion 0.1.2
    */
    @RequestMapping(value="/profile/{id}", method = RequestMethod.DELETE)
    public void profileDelete(Authentication auth, @PathVariable Long id) 
        throws NotFoundException
    {
        User user = getUser(auth);

        if (!profileDAO.remove(id, user))
            throw new NotFoundException("Profile not found");
    }

    /** @api {post} /profile/:id/addevent Suggest event to profile
        @apiName ProfileAddEvent
        @apiParam {Number} id Profile's unique ID
        @apiExample  {json} Example of JSON to upload
            {
              "event": {
                  "@type": "SearchEvent",
                  "id": 728,
              },
              "weight": 0.5,
              "actor": "FooAlgorithm"
            }

        @apiPermission user
        @apiGroup Profiles
        @apiVersion 0.1.2
     */
    @RequestMapping(value="/profile/{id}/addevent", method = RequestMethod.POST)
    public ResponseEntity<Profile>
        profileAddEvent(Authentication auth, @PathVariable Long id,
                        @RequestBody EventRelation suggestedRelation)
        throws NotFoundException, BadRequestException
    {
        User user = getUser(auth);
        
        Profile profile = updateProfile(id, suggestedRelation, false, user);

        return new ResponseEntity<Profile>(profile, HttpStatus.OK);
    }   

    /** @api {post} /profile/:id/validateevent Validate an event for the profile
        @apiName ProfileValidateEvent
        @apiParam {Number} id Profile's unique ID
        @apiExample  {json} Example of JSON to upload
            {
              "event": {
                  "@type": "SearchEvent",
                  "id": 728,
              },
              "weight": 0.9,
            }

        @apiPermission user
        @apiGroup Profiles
        @apiVersion 0.1.2
     */
    @RequestMapping(value="/profile/{id}/validateevent", method = RequestMethod.POST)
    public ResponseEntity<Profile>
        profileValidateEvent(Authentication auth, @PathVariable Long id,
                             @RequestBody EventRelation validatedRelation)
        throws NotFoundException, BadRequestException
    {
        User user = getUser(auth);

        Profile profile = updateProfile(id, validatedRelation, true, user);

        return new ResponseEntity<Profile>(profile, HttpStatus.OK);
    }   

    /** @api {post} /profile/:id/addinformationelement Suggest information element to profile
        @apiName ProfileAddInformationElement
        @apiParam {Number} id Profile's unique ID
        @apiExample  {json} Example of JSON to upload
            {
              "informationelement": {
                  "@type": "Document",
                  "id": 728,
              },
              "weight": 0.22,
              "actor": "FooAlgorithm"
            }

        @apiPermission user
        @apiGroup Profiles
        @apiVersion 0.1.2
     */
    @RequestMapping(value="/profile/{id}/addinformationelement", method = RequestMethod.POST)
    public ResponseEntity<Profile>
        profileAddInformationElement(Authentication auth, @PathVariable Long id,
                                     @RequestBody InformationElementRelation suggestedRelation)
        throws NotFoundException, BadRequestException
    {
        User user = getUser(auth);

        Profile profile = updateProfile(id, suggestedRelation, false, user);

        return new ResponseEntity<Profile>(profile, HttpStatus.OK);
    }   

    /** @api {post} /profile/:id/validateinformationelement Validate an informationelement for the profile
        @apiName ProfileValidateInformationElement
        @apiParam {Number} id Profile's unique ID
        @apiExample  {json} Example of JSON to upload
            {
              "informationelement": {
                  "@type": "Document",
                  "id": 728,
              },
              "weight": 0.9,
            }

        @apiPermission user
        @apiGroup Profiles
        @apiVersion 0.1.2
     */
    @RequestMapping(value="/profile/{id}/validateinformationelement", method = RequestMethod.POST)
    public ResponseEntity<Profile>
        profileValidateInformationElement(Authentication auth, @PathVariable Long id,
                                          @RequestBody InformationElementRelation validatedRelation)
        throws NotFoundException, BadRequestException
    {
        User user = getUser(auth);

        Profile profile = updateProfile(id, validatedRelation, true, user);

        return new ResponseEntity<Profile>(profile, HttpStatus.OK);
    }   
}
