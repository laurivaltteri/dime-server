/*
  Copyright (c) 2016 University of Helsinki

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

import com.fasterxml.jackson.annotation.*;
import org.springframework.data.jpa.domain.AbstractPersistable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

/**
   Class representing a DiMe profile.
*/
@JsonInclude(value=JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="@type")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "new"})
@Entity
public class Profile extends AbstractPersistable<Long> {
    public Profile(String name) {
        this.name = name;
        this.suggestedEvents = new ArrayList<EventRelation>();
        this.validatedEvents = new ArrayList<EventRelation>();
        this.suggestedInformationElements = new ArrayList<InformationElementRelation>();
        this.validatedInformationElements = new ArrayList<InformationElementRelation>();
    }

    public Profile() {
        this("");
    }

    public void copyIdFrom(Profile p) {
        setId(p.getId());
    }

    /** Date and time when the object was first uploaded via the
        external API - automatically generated by DiMe.
     */
    public Date timeCreated;

    /** Date and time when the objects was last modified via the
        external API - automatically generated by DiMe.
    */
    public Date timeModified;
    
    /** Profile name: a free-form name specified by the user. */
    @Column(columnDefinition="text")
    public String name;

    /** List of saved searches. */
    @ElementCollection(targetClass = String.class)
    public List<String> searchKeywords;

    /** Tags, lists of associated tag strings. */
    @ElementCollection(targetClass = String.class)
    public List<String> tags;

    /** List of validated events, i.e. user has confirmed they belong to the profile. */
    @OneToMany(cascade=CascadeType.ALL)
    public List<EventRelation> validatedEvents;

    /** List of suggested events, i.e. suggested as belonging to the
     * profile by an algorithm. (Also later confirmed events will be here.) */
    @OneToMany(cascade=CascadeType.ALL)
    public List<EventRelation> suggestedEvents;

    /** List of validated information elements, i.e. user has confirmed
     * they belong to the profile. */
    @OneToMany(cascade=CascadeType.ALL, orphanRemoval=true)
    public List<InformationElementRelation> validatedInformationElements;

    /** List of suggested information elements, i.e. suggested as
     * belonging to the profile by an algorithm. (Also later confirmed
     * ones will be here.) */
    @OneToMany(cascade=CascadeType.ALL, orphanRemoval=true)
    public List<InformationElementRelation> suggestedInformationElements;

    /** Helper method for finding matching relations in the given
        list. Matches are based on refering to the same object and
        having the same actor. Hence, two relations for the same
        object but with different actors are considered as different.

        @param relation A relation to find matches for
        @param relationList The list to search
        @return List of matching relations in the given list
     */
    protected <R extends DiMeDataRelation> List<R> findRelation(R relation,
                                                                List<R> relationList) 
    {
        List<R> ret = new ArrayList<R>();
        for (R r : relationList) {
            boolean sameData = r.getData() == relation.getData() || 
                (relation.getData().getId() != null && r.getData().getId() != null &&
                 relation.getData().getId().equals(r.getData().getId()));

            if (sameData && r.actor.equals(relation.actor))
                ret.add(r);
        }
        return ret;
    }

    /** Helper method for adding a relation, e.g. suggested event. 
        @param relation Relation to add
        @param relationList List to add to
    */
    protected <R extends DiMeDataRelation> void addRelation(R relation, List<R> relationList) {
        List<R> found = findRelation(relation, relationList);
        assert(found.size() <= 1);
        for (R r : found)
            relationList.remove(r);

        relationList.add(relation);
    }

    /** Add suggested event relation.

        @param suggestedRelation relation to add
    */
    public void addEvent(EventRelation suggestedRelation) {
        addRelation(suggestedRelation, suggestedEvents);
    }

    /** Add suggested event relation.

        @param event Event which the relation concerns
        @param weight Weight of the relation
        @param actor Actor of the relation, i.e. application that generated it
    */
    public void addEvent(Event event, double weight, String actor) {
        addEvent(new EventRelation(event, weight, actor));
    }

    /** Add validated event relation.

        @param validationRelation relation to add
    */
    public void validateEvent(EventRelation validationRelation) {
        validationRelation.validated = true;
        addRelation(validationRelation, validatedEvents);

        List<EventRelation> found = findRelation(validationRelation, suggestedEvents);
        for (EventRelation r : found)
            r.validated = true;
    }

    /** Add validated event relation.

        @param event Event which the relation concerns
        @param weight Weight of the relation
        @param actor Actor of the relation, i.e. application that generated it
    */
    public void validateEvent(Event event, double weight, String actor) {
        validateEvent(new EventRelation(event, weight, actor, true));
    }

    /** Add suggested information element relation.

        @param suggestedRelation relation to add
    */
    public void addInformationElement(InformationElementRelation suggestedRelation) {
        addRelation(suggestedRelation, suggestedInformationElements);
    }

    /** Add suggested information element relation.

        @param elem Information element to which the relation concerns
        @param weight Weight of the relation
        @param actor Actor of the relation, i.e. application that generated it
    */
    public void addInformationElement(InformationElement elem, double weight, String actor) {
        addInformationElement(new InformationElementRelation(elem, weight, actor));
    }

    /** Add validated information element relation.

        @param validationRelation relation to add
    */
    public void validateInformationElement(InformationElementRelation validationRelation) {
        validationRelation.validated = true;
        addRelation(validationRelation, validatedInformationElements);

        List<InformationElementRelation> found = 
            findRelation(validationRelation, suggestedInformationElements);
        for (InformationElementRelation r : found)
            r.validated = true;
    }

    /** Add validated information element relation.

        @param elem Information element to which the relation concerns
        @param weight Weight of the relation
        @param actor Actor of the relation, i.e. application that generated it
    */
    public void validateInformationElement(InformationElement elem, double weight, String actor) {
        validateInformationElement(new InformationElementRelation(elem, weight, actor, true));
    }

    /** User that owns the profile - automatically generated by DiMe.
    */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    public User user;
}
