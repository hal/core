import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Heiko Braun
 * @since 27/03/15
 */
public class PreviewGen {
    private final String in;
    private final String out;

    public static void main(String[] args) throws Exception
    {
        String in = args[0];
        String out = args[1];

        if(in==null || out==null)
        {
            System.out.println("Usage: PreviewGen <input.csv> <output.directory>");
            System.exit(-1);
        }


        PreviewGen gen = new PreviewGen(in, out);
        gen.start();

    }

    public PreviewGen(String in, String out) {
        this.in = in;
        this.out = out;
    }

    public void start() throws Exception {

        File dir = new File(out);
        if(!dir.exists())
            dir.mkdirs();


        Stream<String> lines = Files.lines(Paths.get(in));
        lines
                .filter(s -> !s.startsWith("#"))
                .forEach(line -> genFile(dir, line));

        lines.close();
        System.out.println("Done");
    }

    private void genFile(File out, String line) {

        int col = 0;
        int i = line.indexOf(",");
        if(i==-1) return;

        Content c = new Content();

        while(col<3) {
            switch (col) {
                case 0:
                    c.title = line.substring(0, i);
                    col++;
                    break;
                case 1:
                    int prev = i;
                    i = line.indexOf(",", prev+1);
                    c.token = line.substring(prev+1, i);
                    col++;
                    break;
                case 2:
                    c.desc = line.substring(i+1, line.length());
                    col++;
                    break;
                default:
                    // ignore
            }
        }

        if(c.isComplete()) {

            writeFile(c, content -> {

                StringBuilder sb = new StringBuilder();

                sb.append("<div class='preview-content'>");

                sb.append("<h1>").append(content.title).append("</h1>");
                sb.append(content.desc);

                sb.append("</div>");
                return sb.toString();
            } );
        }
    }

    private void writeFile(Content c, Function<Content, String> f) {
        try {
            String fileName = c.token + ".html";
            FileWriter fileWriter = new FileWriter(new File(out, fileName));
            fileWriter.write(f.apply(c));
            fileWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    class Content {
        String title;
        String token;
        String desc;

        boolean isComplete() {
            return (token!=null && desc!=null)
                    && (!token.isEmpty() && !desc.isEmpty());
        }
    }

}
