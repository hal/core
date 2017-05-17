package org.jboss.as.console.mbui.model.mapping;

import com.allen_sauer.gwt.log.client.Log;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import static org.jboss.dmr.client.ModelDescriptionConstants.ADDRESS;
import org.jboss.dmr.client.ModelNode;
import org.useware.kernel.gui.behaviour.StatementContext;

/**
 * Address mapping of domain references used within the interface model.
 * Typically as part of a {@link DMRMapping}.
 * <p/>
 * The mapping currently supports three different types of address value declarations:
 *
 * <ul>
 *     <li>token: key=value</li>
 *     <li>value expression: some.key={value.ref}</li>
 *     <li>token expression: {some.tuple}</li>
 * </ul>
 *
 * A token is a fully qualified address tuple without any parameters, i.e. "subsystem=default".<br/>
 * A value expression carries a parameter for one part of the tuple, i.e. "subsystem={name}".<br/>
 * A token expression references a full tuple, with both the key and the value part, i.e "{selected.profile}/subsystem=datasources".<p/>
 *
 * All expression are resolved against the {@link StatementContext}.
 *
 * @author Heiko Braun
 * @date 9/23/11
 */
public class AddressMapping {

    private List<Token> address = new LinkedList<Token>();
    private int countedWildcards = -1;
    private Set<String> requiredStatements;


    public AddressMapping(List<Token> tuple) {
        this.address = tuple;
    }

    public void add(String parent, String child)
    {
        address.add(new Token(parent, child));
    }

    public ModelNode asResource(StatementContext context, String... wildcards) {
        return asResource(new ModelNode(), context, wildcards);
    }

    /**
     * Parses the address declaration for tokens
     * that need to be resolved against the statement context.
     *
     * @return
     */
    public Map<String, Integer> getRequiredStatements() {

        Map<String, Integer> required = new HashMap<String,Integer>();
        for(Token token : address)
        {
            if(!token.hasKey())
            {
                // a single token or token expression
                // These are currently skipped: See asResource() parsing logic
                continue;
            }
            else
            {
                // a value expression. key and value of the expression might be resolved
                // TODO: keys are not supported: Do we actually need that?
                String value_ref = token.getValue();
                if(value_ref.startsWith("{"))
                {
                    value_ref = value_ref.substring(1, value_ref.length()-1);
                    if(!required.containsKey(value_ref))
                    {
                        required.put(value_ref, 1);
                    }
                    else
                    {
                        Integer count = required.get(value_ref);
                        ++count;
                        required.put(value_ref, count);
                    }
                }

            }
        }
        return required;
    }

    class Memory<T> {
        Map<String, LinkedList<T>> values = new HashMap<String, LinkedList<T>>();
        Map<String, Integer> indexes = new HashMap<String, Integer>();

        boolean contains(String key) {
            return values.containsKey(key);
        }

        void memorize(String key, LinkedList<T> resolved)
        {
            int startIdx = resolved.isEmpty() ? 0 : resolved.size() - 1;

            values.put(key, resolved);
            indexes.put(key, startIdx);
        }

        /**
         * The result from the statement scope hierarchy are resolved from child towards parents.
         * hence we need to map it backwards into the address tokens.
         *
         * @param key
         * @return
         */
        T next(String key) {

            T result = null;

            LinkedList<T> items = values.get(key);
            Integer idx = indexes.get(key);

            if(!items.isEmpty() && idx>=0)
            {
                result = items.get(idx);
                indexes.put(key, --idx);
            }

            return result;
        }
    }

    public ModelNode asResource(ModelNode baseAddress, StatementContext context, String... wildcards) {

        ModelNode model = new ModelNode();
        model.get(ADDRESS).set(baseAddress);

        int wildcardCount = 0;

        Memory<String[]> tupleMemory = new Memory<String[]>();
        Memory<String> valueMemory = new Memory<String>();

        for(Token token: address)   // TODO: resolve ambiguous keys across context hierarchy
        {

            if(!token.hasKey())
            {
                // a single token or token expression

                String token_ref = token.getValue();
                String[] resolved_value = null;

                if(token_ref.startsWith("{"))
                {
                    token_ref = token_ref.substring(1, token_ref.length()-1);

                    if(!tupleMemory.contains(token_ref))
                    {
                        tupleMemory.memorize(token_ref, context.collectTuples(token_ref));
                    }

                    resolved_value = tupleMemory.next(token_ref);
                }
                else
                {
                    assert token_ref.contains("=") : "Invalid token expression "+token_ref;
                    resolved_value = token_ref.split("=");
                }

                // TODO: is it safe to suppress token expressions that cannot be resolved?
                // i.e /{selected.profile}/subsystem=foobar/ on a standalone server?

                if(null==resolved_value)
                {
                    Log.warn("Suppress token expression '"+token_ref+"'. It cannot be resolved");
                }
                else
                {
                    model.get(ADDRESS).add(resolved_value[0], resolved_value[1]);
                }

            }
            else
            {
                // a value expression. key and value of the expression might be resolved

                String key_ref = token.getKey();
                String value_ref = token.getValue();

                String resolved_key = null;
                String resolved_value = null;

                if(key_ref.startsWith("{"))
                {
                    key_ref = key_ref.substring(1, key_ref.length()-1);

                    if(!valueMemory.contains(key_ref))
                        valueMemory.memorize(key_ref, context.collect(key_ref));

                    resolved_key = valueMemory.next(key_ref);
                }
                else
                {
                    resolved_key = key_ref;
                }

                if(value_ref.startsWith("{"))
                {
                    value_ref = value_ref.substring(1, value_ref.length()-1);

                    int entries = 0;
                    if(!valueMemory.contains(value_ref)) {
                        LinkedList<String> values = context.collect(value_ref);
                        entries = values.size();
                        valueMemory.memorize(value_ref, values);
                    }

                    if (entries > 1 && value_ref.equalsIgnoreCase("addressable.group")) {

                        ModelNode addresses = new ModelNode();
                        addresses.setEmptyList();
                        while ((resolved_value = valueMemory.next(value_ref)) != null) {
                            addresses.add(resolved_value);
                        }
                        model.get(ADDRESS).add(resolved_key, addresses);
                        continue;
                    }
                    resolved_value= valueMemory.next(value_ref);
                }
                else
                {
                    resolved_value = value_ref;
                }

                //assert resolved_key!=null : "The key '"+key_ref+"' cannot be resolved";
                //assert resolved_value!=null : "The value '"+value_ref+"' cannot be resolved";

                if(resolved_key==null) resolved_key = "_blank";
                if(resolved_value==null) resolved_value = "_blank";

                // wildcards
                String addressValue = resolved_value;

                if ("*".equals(resolved_value) && wildcards.length > 0 && wildcardCount < wildcards.length)
                {
                    addressValue = wildcards[wildcardCount];
                    wildcardCount++;
                }

                model.get(ADDRESS).add(resolved_key, addressValue);
            }

        }

        return model;
    }

    public static List<Token> parseAddressString(String value) {
        List<Token> address = new LinkedList<Token>();

        if(value.equals("/")) // default parent value
            return address;

        StringTokenizer tok = new StringTokenizer(value, "/");
        while(tok.hasMoreTokens())
        {
            String nextToken = tok.nextToken();
            if(nextToken.contains("="))
            {
                String[] split = nextToken.split("=");
                address.add(new Token(split[0], split[1]));
            }
            else
            {
                address.add(new Token(nextToken));
            }

        }
        return address;
    }

    public static class Token {
        String key;
        String value;

        Token(String key, String value) {
            this.key = key;
            this.value = value;
        }

        Token(String value) {
            this.key = null;
            this.value = value;
        }

        boolean hasKey() {
            return key!=null;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            if(hasKey())
                return key+"="+value;
            else
                return value;
        }
    }

    public static AddressMapping fromString(String address) {
        assert address!=null : "Address cannot be null";
        return new AddressMapping(AddressMapping.parseAddressString(address));
    }

    public static class StringTokenizer {
        private final String deli;
        private final String s;
        private final int len;

        private int pos;
        private String next;

        public StringTokenizer(String s, String deli) {
            this.s = s;
            this.deli = deli;
            len = s.length();
        }

        public StringTokenizer(String s) {
            this(s, " \t\n\r\f");

        }

        public String nextToken() {
            if(!hasMoreTokens()) {
                throw new NoSuchElementException();
            }
            String result = next;
            next = null;
            return result;
        }

        public boolean hasMoreTokens() {
            if (next != null) {
                return true;
            }
            // skip leading delimiters
            while (pos < len && deli.indexOf(s.charAt(pos)) != -1) {
                pos++;
            }

            if (pos >= len) {
                return false;
            }

            int p0 = pos++;
            while (pos < len && deli.indexOf(s.charAt(pos)) == -1) {
                pos++;
            }

            next = s.substring(p0, pos++);
            return true;
        }

    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for(Token token: address)
        {
            sb.append("/").append(token);
        }
        return sb.toString();
    }
}

