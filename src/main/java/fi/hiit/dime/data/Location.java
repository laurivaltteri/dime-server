/*
  Copyright (c) 2015 University of Helsinki

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

import javax.persistence.Embeddable;

/**
   Class representing a location on the planet Earth (usually).
*/
@Embeddable
public class Location {
    /** Latitude in degrees
     */
    public Double latitude;

    /** Longitude in degrees
     */
    public Double longitude;

    /** Altitude in metres.
     */
    public Double altitude;

    /** Horizontal accuracy, in metres.
     */
    public Double horizAccuracy;

    /** Vertical accuracy, in metres.
     */
    public Double vertAccuracy;

    /** Bearing in degrees, east of north (0: north, 90: east, 180: south, 270: west).
     */
    public Double bearing;

    /** Speed in metres per second
     */
    public Double speed;

    /** A (relatively) long line of text that quickly describes the location to a human.
     * Example: "Country: Finland, Locality: Helsinki, Neighborhood: Kumpula, Steet: Gadolininkatu
     */
    public String descriptionLine;

}
