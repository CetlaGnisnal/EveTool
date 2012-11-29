@Grab(group='jline', module='jline', version='2.9')
@Grab(group='org.fusesource.jansi', module='jansi', version='1.9')

import jline.*
import org.fusesource.jansi.*
import org.fusesource.jansi.Ansi.Color

import static org.fusesource.jansi.Ansi.Color.*
import static org.fusesource.jansi.Ansi.Erase.FORWARD
import static org.fusesource.jansi.Ansi.ansi

@Singleton
class ScriptConsole {

    static final String LINE_SEPARATOR = System.getProperty("line.separator")
    static final String PROMPT = '➤'
    static final String MSG_SEPARATOR = '┋'
    static final String SPACE = " "

    boolean ansiEnabled = true

    private int cursorMove
    private String lastMessage = ""
    private Ansi lastStatus = null

    Stack<String> category = new Stack<String>() {
        @Override
        public String toString() {
            if (size() == 1) return peek() + MSG_SEPARATOR;
            return this.join(MSG_SEPARATOR) + MSG_SEPARATOR
        }
    }

    private PrintStream out
    private ConsoleReader reader
    private Terminal terminal

    private boolean userInputActive
    private boolean appendCalled = false

    private ScriptConsole() {
        cursorMove = 1

        out = new PrintStream(AnsiConsole.wrapOutputStream(System.out))
        reader = new ConsoleReader(System.in, new OutputStreamWriter(System.out))
        terminal = Terminal.setupTerminal()

        println()
        println()
    }

    boolean isAnsiEnabled() {
        return Ansi.isEnabled() && (terminal != null && terminal.isANSISupported()) && ansiEnabled
    }

    private void assertAllowInput() {
        if (reader == null) {
            throw new IllegalStateException("User input is not enabled, cannot obtain input stream")
        }
    }

    void updateStatus(String msg, String color = null) {
        outputMessage(colorize(msg, color), 1)
    }

    private colorize(String msg, String color) {
        color ? ansi().fg(Color.valueOf(color)).a(msg).reset().toString() : msg
    }

    void addStatus(String msg, String color = null) {
        outputMessage(colorize(msg, color), 0)
        lastMessage = ""
    }

    void addHeader(String msg) {
        if (isAnsiEnabled()) {
            addLine()
            addStatus ansi()
                      .a(Ansi.Attribute.INTENSITY_BOLD)
                      .fg(Color.RED)
                      .a(msg)
                      .reset()
                      .toString()
            addStatus ansi()
                      .a(Ansi.Attribute.INTENSITY_BOLD)
                      .fg(Color.RED)
                      .a("".padRight(80, "="))
                      .reset()
                      .toString()
           addLine()
        } else {
            addStatus(msg)
        }
    }

    void addLine() {
        addStatus ansi().reset().toString()
    }
    void append(String msg) {
        if (userInputActive && !appendCalled) {
            out.print(moveDownToSkipPrompt())
            appendCalled = true;
        }

        if (msg.endsWith(LINE_SEPARATOR)) {
            out.print(msg)
        } else {
            out.println(msg)
        }

        cursorMove = 0
    }

    void echoStatus() {
        if (lastStatus != null) {
            updateStatus(lastStatus.toString());
        }
    }

    private void outputMessage(String msg, int replaceCount) {
        if (msg == null || msg.trim().length() == 0) return

        try {
            if (isAnsiEnabled()) {
                out.print(erasePreviousLine(MSG_SEPARATOR))
                lastStatus = outputCategory(ansi(), MSG_SEPARATOR)
                             .fg(Color.DEFAULT)
                             .a(msg)
                             .reset()
                out.println(lastStatus)
                if (!userInputActive) {
                    cursorMove = replaceCount
                }
            } else {
                if (lastMessage != null && lastMessage.equals(msg)) return

                out.print(MSG_SEPARATOR)
                out.println(msg)
            }
            lastMessage = msg
        }
        finally {
            postPrintMessage()
        }
    }

    private Ansi erasePreviousLine(String categoryName) {
        @SuppressWarnings("hiding") int cursorMove = this.cursorMove
        if (userInputActive) cursorMove++
        if (cursorMove > 0) {
            int moveLeftLength = categoryName.length() + lastMessage.length()
            if (userInputActive) {
                moveLeftLength += PROMPT.length()
            }
            return ansi()
                    .cursorUp(cursorMove)
                    .cursorLeft(moveLeftLength)
                    .eraseLine(FORWARD)

        }
        return ansi()
    }

    private Ansi outputCategory(Ansi ansi, String categoryName) {
        return ansi
                .a(Ansi.Attribute.INTENSITY_BOLD)
                .fg(YELLOW)
                .a(categoryName)
                .a(SPACE)
                .a(Ansi.Attribute.INTENSITY_BOLD_OFF)
    }

    private void postPrintMessage() {
        appendCalled = false
        if (userInputActive) {
            showPrompt()
        }
    }

    String showPrompt() {
        String prompt = isAnsiEnabled() ? ansiPrompt(PROMPT).toString() : PROMPT
        return showPrompt(prompt)
    }

    private Ansi ansiPrompt(String prompt) {
        return ansi()
                .a(Ansi.Attribute.INTENSITY_BOLD)
                .fg(YELLOW)
                .a(prompt)
                .a(Ansi.Attribute.INTENSITY_BOLD_OFF)
                .fg(DEFAULT)
    }

    private Ansi moveDownToSkipPrompt() {
        return ansi()
                .cursorDown(1)
                .cursorLeft(PROMPT.length());
    }

    String showPrompt(String prompt) {
        cursorMove = 0
        if (!userInputActive) {
            return readLine(prompt)
        }

        out.print(prompt)
        flush()
        return null
    }

    private String readLine(String prompt) {
        assertAllowInput()
        userInputActive = true
        try {
            reader.readLine(prompt)
        } catch (IOException e) {
            throw new RuntimeException("Error reading input: " + e.getMessage())
        } finally {
            userInputActive = false
        }
    }

    String userInput(String msg) {
        // Add a space to the end of the message if there isn't one already.
        if (!msg.endsWith(" ") && !msg.endsWith("\t")) {
            msg += ' '
        }

        lastMessage = "";
        msg = isAnsiEnabled() ? outputCategory(ansi(), PROMPT).fg(BLUE).a(msg).fg(DEFAULT).toString() : msg
        try {
            return readLine(msg)
        } finally {
            out.print(erasePreviousLine(MSG_SEPARATOR))
            cursorMove = 1
        }
    }

    String userInput(String message, Collection<String> validResponses, boolean strict = true) {
        if (validResponses == null) {
            return userInput(message);
        }

        String question = createQuestion(message, validResponses);
        String response = userInput(question);
        for (String validResponse : validResponses) {
            if (response != null && response.equalsIgnoreCase(validResponse)) {
                return response;
            }
        }

        if(!strict) return response

        cursorMove = 0;
        return userInput("Invalid input. Must be one of ", validResponses);
    }

    private String createQuestion(String message, Collection<String> validResponses) {
        return message + " [" + validResponses.join(', ')+ "]:";
    }

    private void erase(PrintStream printStream, int length) {
        printStream.print(ansi()
                .eraseLine(Ansi.Erase.BACKWARD).cursorLeft(length))
    }

    void flush() {
        out.flush();
    }

}
