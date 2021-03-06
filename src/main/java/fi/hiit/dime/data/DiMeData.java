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

package fi.hiit.dime.data;

import fi.hiit.dime.authentication.User;
import fi.hiit.dime.search.WeightedKeyword;

import com.fasterxml.jackson.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.AbstractPersistable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import java.util.List;

/** Parent object of all DiMe database objects.
*/
@JsonInclude(value=JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="@type")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "new"})
@JsonSubTypes({
            @JsonSubTypes.Type(value=fi.hiit.dime.data.Document.class, name="Document"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.ScientificDocument.class, name="ScientificDocument"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.Message.class, name="Message"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.Person.class, name="Person"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.FeedbackEvent.class, name="FeedbackEvent"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.BookmarkEvent.class, name="BookmarkEvent"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.WebDocument.class, name="WebDocument"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.DesktopEvent.class, name="DesktopEvent"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.ReadingEvent.class, name="ReadingEvent"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.SummaryReadingEvent.class, name="SummaryReadingEvent"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.MessageEvent.class, name="MessageEvent"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.SearchEvent.class, name="SearchEvent"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.FunfEvent.class, name="FunfEvent"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.CalendarEvent.class, name="CalendarEvent"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.IntentModelEvent.class, name="IntentModelEvent"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.PhysicalEvent.class, name="PhysicalEvent"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.HealthTrackerEvent.class, name="HealthTrackerEvent"),
})
@MappedSuperclass
public class DiMeData extends AbstractPersistable<Long> {
    private static final Logger LOG = 
        LoggerFactory.getLogger(DiMeData.class);

    /** An optional identifying unique string field that can be used
        by the application or logger. The value can be any unique text
        string, and is entirely up to the application developer, but
        make sure it is unique. If you upload another object with the
        same appId later (for the same user) it will replace the old
        one (as long as it is of the same exact class).
    */
    public String appId;

    public void copyIdFrom(DiMeData e) {
        setId(e.getId());
    }

    public void resetId() {
        setId(null);
    }

    /** Date and time when the object was first uploaded via the
        external API - automatically generated by DiMe.
     */
    public Date timeCreated;

    /** Date and time when the objects was last modified via the
        external API - automatically generated by DiMe.
    */
    public Date timeModified;

    /** User associated with the object - automatically generated by
        DiMe.
    */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    public User user;

    /** Detailed data type according to the Semantic Desktop ontology:
        http://www.semanticdesktop.org/ontologies/2007/03/22/nfo
     */
    public String type;

    /** Method to call when ever a new object has been uploaded, e.g.
        to clean up user provided data, or perform some house keeping
        before storing in the database.
    */
    public void autoFill() {}

    @Transient
    public Float score;

    @Transient
    public List<WeightedKeyword> weightedKeywords;

    /** List of user-specified tags, interpretation depends on the
        application.
    */
    @OneToMany(cascade=CascadeType.ALL)
    public List<Tag> tags;

    /** Return true if this object has tags

        @return True if there are tags.
    */
    public boolean hasTags() {
        return tags != null && tags.size() > 0;
    }

    /** Add a tag to the object.
        @param tag The tag object to add
    */
    public void addTag(Tag tag) {
        if (tags == null)
            tags = new ArrayList<Tag>();

        Tag oldTag = getTag(tag);
        if (oldTag != null) 
            tags.remove(oldTag);

        tags.add(tag);
    }

    /** Remove tags matching a template.  Matching is defined as in
        {@link fi.hiit.dime.data.Tag#matches(Tag)}.

        @param template Tag template to match
        @return Number of tags removed
    */
    public int removeMatchingTags(Tag template) {
        int count = 0;

        if (template == null)
            return count;

        Iterator<Tag> it = tags.iterator();
        while (it.hasNext()) {
            Tag tag = it.next();
            if (tag.matches(template)) {
                it.remove();
                count++;
            }
        }
        return count;
    }

    /** Return a single Tag which is equal to the given tag. Equality
        is defined as in {@link fi.hiit.dime.data.Tag#tagEquals(Tag)}.

        @param tag Tag to equal
        @return The corresponding Tag object, or null if not found
    */
    public Tag getTag(Tag tag) {
        for (Tag t : tags)
            if (t.tagEquals(tag))
                return t;

        return null;
    }

    /** Return a list of tags matching the given tag template. 
        Matching is defined as in {@link fi.hiit.dime.data.Tag#matches(Tag)}.

        @param template The tag template to match
        @return A list of matching tags, an empty list if none are found
    */
    public List<Tag> getMatchingTags(Tag template) {
        List<Tag> ret = new ArrayList<Tag>();

        if (tags == null || template == null)
            return ret;  // return empty list 

        for (Tag t : tags)
            if (t.matches(template))
                ret.add(t);

        return ret;
    }    

    public boolean hasMatchingTag(String tagText) {
        return hasMatchingTag(new Tag(tagText));
    }

    /** Checks if there are tags matching the given template 
        
        @param template The template to match
        @return True if there are one or more matches
    */
    public boolean hasMatchingTag(Tag template) {
        if (tags == null || template == null)
            return false;
        
        for (Tag t : tags) {
            if (t.matches(template))
                return true;
        }
        return false;
    }

    public static <T extends DiMeData> T makeStub(T data, Class<T> dataType) {
        try {
            T stub = dataType.newInstance();
            stub.setId(data.getId());
            return stub;
        } catch (InstantiationException|IllegalAccessException ex) {
            LOG.error("Unable to create stub of class {}!", dataType.getName());
            return null;
        }
    }
}
