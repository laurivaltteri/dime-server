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
   Class representing a rectangle in a two dimensions (maps to the ReadingRect struct in PeyeDF).
*/
@Embeddable
public class Rect {
    /** Position of the origin of this rectangle (origin: bottom left).
     */
    public Point origin;

    /** Size of this rectangle.
     */
    public Size size;

    /** Page (starting from 0) on which this rectangle appears.
     */
    public int pageIndex;

    /** Reading class of this rectangle, or why is it marked (refer to the CLASS_* constants, Importance enum in PeyeDF).
     */
    public int readingClass;

    /** Source of classification for this rectangle, or what marked it (refer to the CLASSSOURCE_* constants, ClassSource enum in PeyeDF.
     */
    public int classSource;

    /** Unspecified reading class
     */
    public static final int CLASS_UNSET = 0;

    /** Class for reading viewport rectangles (standard without eye tracking).
     */
    public static final int CLASS_VIEWPORT = 10;

    /** A paragraph (probably) exists at this point, can overlap with other paragraph rectangles.
     */
    public static final int CLASS_PARAGRAPH_FLOATING = 13;

    /** A paragraph (probably) exists at this point, tries to cover a whole paragraph (no overlapping).
     */
    public static final int CLASS_PARAGRAPH_UNITED = 14;

    /** Class for eye tracking read text.
     */
    public static final int CLASS_READ = 20;

    /** Class for eye tracking "interesting" rectangles.
     */
    public static final int CLASS_INTERESTING = 30;

    /** Class for eye tracking "critical" rectangles.
     */
    public static final int CLASS_CRITICAL = 40;

    /** Class source for rectangles marked from an unkown source.
     */ 
    public static final int CLASSSOURCE_UNSET = 0;
    
    /** Class source for rectangles marked by the UI as viewports.
     */ 
    public static final int CLASSSOURCE_VIEWPORT = 1;

    /** Class source for rectangles marked by click.
     */
    public static final int CLASSSOURCE_CLICK = 2;
    
    /** Class source for rectangles marked by eye tracking.
     */
    public static final int CLASSSOURCE_EYE = 3;

}
