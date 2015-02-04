import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Heiko Braun
 * @since 04/02/15
 */
public class PropTest {
    public static void main(String[] args) throws Exception {
        File halCoreDir = new File("/Users/hbraun/dev/prj/hal/core/");
        assert halCoreDir.exists();

        File propsDir = new File(halCoreDir, "gui/src/main/java/org/jboss/as/console/client/core/");
        File constantRef = new File(halCoreDir, "gui/src/main/java/org/jboss/as/console/client/core/UIConstants_en.properties");
        File messageRef = new File(halCoreDir, "gui/src/main/java/org/jboss/as/console/client/core/UIMessages.properties");


        compareBundles(constantRef, "UIConstants_", propsDir);
        compareBundles(messageRef, "UIMessages_", propsDir);

    }

    private static void compareBundles(File referenceBundle, final String filter, File propsDir) throws Exception {
        Set<String> keys = getKeys(referenceBundle);

        try(BufferedReader br = new BufferedReader(new FileReader(referenceBundle))) {
            for(String line; (line = br.readLine()) != null; ) {
                keys.add(line.split("=")[0]);
            }
        }

        String[] constants = propsDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(filter) && name.indexOf(".bak")==-1;
            }
        });


        for (String lang : constants) {


            Set<String> target = getKeys(new File(propsDir, lang));
            assert keys.size() > target.size();
            List<String> copy = new ArrayList<>(keys);
            copy.removeAll(target);

            if(copy.size()>0) {
                List<String> sorted = new ArrayList<>(copy);
                Collections.sort(sorted);
                System.out.println("----"+lang+"-----");
                for (String item : sorted) {
                    System.out.println(item);
                }
                System.out.println("-------------\n");
            }
        }
    }

    public static Set<String> getKeys(File f) throws Exception {
        Set<String> keys = new HashSet<>();

        try(BufferedReader br = new BufferedReader(new FileReader(f))) {
            for(String line; (line = br.readLine()) != null; ) {
                if(!line.equals("") && !line.startsWith("#"))
                    keys.add(line.split("=")[0]);
            }
        }

        return keys;
    }
}
