/*
 * <p>GPL Dislaimer</p>
 * <p>
 * "Reversi by Frank Kopp"
 * Copyright 2003, 2004, 2005, 2006 Frank Kopp
 * mail-to:frank@familie-kopp.de
 *
 * This file is part of "Reversi by Frank Kopp".
 *
 * "Reversi by Frank Kopp" is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * "Reversi by Frank Kopp" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with "Reversi by Frank Kopp"; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * </p>
 *
 *
 */

package fko.reversi.ui.ReversiGUI;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a Method, the list of arguments to be passed
 * to that method, and the object on which the method is to be invoked.
 * The invoke() method invokes the method.  The actionPerformed() method
 * does the same thing, allowing this class to implement ActionListener
 * and be used to respond to ActionEvents generated in a GUI or elsewhere.
 * The static parse() method parses a string representation of a method
 * and its arguments.
 */
public class Command implements ActionListener {

    private Method m;       // The method to be invoked
    private Object target;  // The object to invoke it on
    private Object[] args;  // The arguments to pass to the method

    // An empty array; used for methods with no arguments at all.
    static final Object[] nullargs = new Object[]{};

    /**
     * This constructor creates a Command object for a no-arg method
     */
    public Command(Object aTarget, Method aMethod) {
        this(aTarget, aMethod, nullargs);
    }

    /**
     * This constructor creates a Command object for a method that takes the
     * specified array of arguments.  Note that the parse() method provides
     * another way to create a Command object
     */
    public Command(Object aTarget, Method aMethod, Object[] theArgs) {
        this.target = aTarget;
        this.m = aMethod;
        this.args = theArgs;
    }

    /**
     * Invoke the Command by calling the method on its target, and passing
     * the arguments.  See also actionPerformed() which does not throw the
     * checked exceptions that this method does.
     */
    public void invoke()
            throws IllegalAccessException, InvocationTargetException {
        m.invoke(target, args);  // Use reflection to invoke the method
    }

    /**
     * This method implements the ActionListener interface.  It is like
     * invoke() except that it catches the exceptions thrown by that method
     * and rethrows them as an unchecked RuntimeException
     */
    public void actionPerformed(ActionEvent e) {
        try {
            invoke();                           // Call the invoke method
        }
        catch (InvocationTargetException ex) {  // but handle the exceptions
            throw new RuntimeException("Command: " +
                                       ex.getTargetException().toString());
        }
        catch (IllegalAccessException ex) {
            throw new RuntimeException("Command: " + ex.toString());
        }
    }

    /**
     * This static method creates a Command using the specified target object,
     * and the specified string.  The string should contain method name
     * followed by an optional parenthesized comma-separated argument list and
     * a semicolon.  The arguments may be boolean, integer or double literals,
     * or double-quoted strings.  The parser is lenient about missing commas,
     * semicolons and quotes, but throws an IOException if it cannot parse the
     * string.
     */
    public static Command parse(Object aTarget, String text) throws IOException {
        String methodname;                 // The name of the method
        List args = new ArrayList(10);  // Hold arguments as we parse them.
        List types = new ArrayList(10); // Hold argument types.

        // Convert the string into a character stream, and use the
        // StreamTokenizer class to convert it into a stream of tokens
        StreamTokenizer t = new StreamTokenizer(new StringReader(text));

        // The first token must be the method name
        int c = t.nextToken();  // read a token
        if (c != StreamTokenizer.TT_WORD)     // check the token type
        {
            throw new IOException("Missing method name for command");
        }
        methodname = t.sval;    // Remember the method name

        // Now we either need a semicolon or a open paren
        c = t.nextToken();
        if (c == '(') { // If we see an open paren, then parse an arg list
            for (; ;) {                   // Loop 'till end of arglist
                c = t.nextToken();      // Read next token

                if (c == ')') {         // See if we're done parsing arguments.
                    c = t.nextToken();  // If so, parse an optional semicolon
                    if (c != ';') {
                        t.pushBack();
                    }
                    break;              // Now stop the loop.
                }

                // Otherwise, the token is an argument; figure out its type
                if (c == StreamTokenizer.TT_WORD) {
                    // If the token is an identifier, parse boolean literals,
                    // and treat any other tokens as unquoted string literals.
                    if (t.sval.equals("true")) {       // Boolean literal
                        args.add(Boolean.TRUE);
                        types.add(boolean.class);
                    } else if (t.sval.equals("false")) { // Boolean literal
                        args.add(Boolean.FALSE);
                        types.add(boolean.class);
                    } else {                             // Assume its a string
                        args.add(t.sval);
                        types.add(String.class);
                    }
                } else if (c == '"') {         // If the token is a quoted string
                    args.add(t.sval);
                    types.add(String.class);
                } else if (c == StreamTokenizer.TT_NUMBER) { // If the token is a number
                    //noinspection NumericCastThatLosesPrecision
                    int i = (int) t.nval;
                    //noinspection FloatingPointEquality,ImplicitNumericConversion
                    if (i == t.nval) {           // Check if its an integer
                        // Note: this code treats a token like "2.0" as an int!
                        args.add(i);
                        types.add(int.class);
                    } else {                       // Otherwise, its a double
                        args.add(t.nval);
                        types.add(double.class);
                    }
                } else {                        // Any other token is an error
                    throw new IOException("Unexpected token " + t.sval +
                                          " in argument list of " +
                                          methodname + "().");
                }

                // Next should be a comma, but we don't complain if its not
                c = t.nextToken();
                if (c != ',') {
                    t.pushBack();
                }
            }
        } else if (c != ';') { // if a method name is not followed by a paren
            t.pushBack();    // then allow a semi-colon but don't require it.
        }

        // We've parsed the argument list.
        // Next, convert the lists of argument values and types to arrays
        Object[] argValues = args.toArray();
        Class[] argtypes = (Class[]) types.toArray(new Class[argValues.length]);

        // At this point, we've got a method name, and arrays of argument
        // values and types.  Use reflection on the class of the target object
        // to find a method with the given name and argument types.  Throw
        // an exception if we can't find the named method.
        Method method;
        try {
            method = aTarget.getClass().getMethod(methodname, argtypes);
        }
        catch (Exception e) {
            throw new IOException("No such method found, or wrong argument " +
                                  "types: " + methodname);
        }

        // Finally, create and return a Command object, using the target object
        // passed to this method, the Method object we obtained above, and
        // the array of argument values we parsed from the string.
        return new Command(aTarget, method, argValues);
    }

}
