package org.jboss.as.console.client.administration.role;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.rbac.StandardRole;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.SuggestBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

/**
 * @author Harald Pehl
 * @date 07/25/2013
 */
public class AddPrincipalWizard implements IsWidget {

    private final RoleAssignmentPresenter presenter;
    private final StandardRole role;
    private final RoleAssignment roleAssignment;
    private final Principal.Type principalType;

    public AddPrincipalWizard(final RoleAssignmentPresenter presenter, final StandardRole role,
            final RoleAssignment roleAssignment, final Principal.Type principalType) {
        this.presenter = presenter;
        this.role = role;
        this.roleAssignment = roleAssignment;
        this.principalType = principalType;
    }

    @Override
    public Widget asWidget() {

        // To have something to play with...
        MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
        oracle.add("stuartwdouglas");
        oracle.add("kabir");
        oracle.add("dmlloyd");
        oracle.add("jaikiran");
        oracle.add("emuckenhuber");
        oracle.add("tdiesler");
        oracle.add("darranl");
        oracle.add("ctomc");
        oracle.add("baileyje");
        oracle.add("n1hility");
        oracle.add("wolfc");
        oracle.add("scottmarlow");
        oracle.add("jamezp");
        oracle.add("pferraro");
        oracle.add("jmesnil");
        oracle.add("jfclere");
        oracle.add("maeste");
        oracle.add("rachmatowicz");
        oracle.add("pgier");
        oracle.add("rmaucher");
        oracle.add("OndraZizka");
        oracle.add("rhusar");
        oracle.add("asoldano");
        oracle.add("ssilvert");
        oracle.add("ropalka");
        oracle.add("ochaloup");
        oracle.add("anilsaldhana");
        oracle.add("aloubyansky");
        oracle.add("heiko-braun");
        oracle.add("sguilhen");
        oracle.add("dpospisil");
        oracle.add("mmoyses");
        oracle.add("emmartins");
        oracle.add("kwart");
        oracle.add("vratsel");
        oracle.add("maasvdberg");
        oracle.add("paulrobinson");
        oracle.add("jhjaggars");
        oracle.add("alesj");
        oracle.add("jesperpedersen");
        oracle.add("ALRubinger");
        oracle.add("pskopek");
        oracle.add("bosschaert");
        oracle.add("wfink");
        oracle.add("starksm64");
        oracle.add("rsvoboda");
        oracle.add("andytaylor");
        oracle.add("aslakknutsen");
        oracle.add("jharting");
        oracle.add("baranowb");
        oracle.add("Mogztter");
        oracle.add("smcgowan");
        oracle.add("istudens");
        oracle.add("maerqiang");
        oracle.add("tristantarrant");
        oracle.add("fharms");
        oracle.add("dosoudil");
        oracle.add("sfcoy");
        oracle.add("pjanouse");
        oracle.add("madhumita");
        oracle.add("jsight");
        oracle.add("jeffzhang");
        oracle.add("soul2zimate");
        oracle.add("marschall");
        oracle.add("dobozysaurus");
        oracle.add("doctau");
        oracle.add("tomjenkinson");
        oracle.add("KurtStam");
        oracle.add("pkremens");
        oracle.add("ecki");
        oracle.add("stliu");
        oracle.add("mbogoevici");
        oracle.add("hpehl");
        oracle.add("miclark");
        oracle.add("jmartisk");
        oracle.add("mmatloka");
        oracle.add("chengfang");
        oracle.add("gunnarmorling");
        oracle.add("BrentDouglas");
        oracle.add("bobmcwhirter");
        oracle.add("galderz");
        oracle.add("clebertsuconic");
        oracle.add("okulikov");
        oracle.add("spolti");
        oracle.add("adinn");
        oracle.add("metlos");
        oracle.add("btison");
        oracle.add("motaboy");
        oracle.add("stalep");
        oracle.add("jbertram");
        oracle.add("patriot1burke");
        oracle.add("navssurtani");
        oracle.add("rreimann");
        oracle.add("goldmann");
        oracle.add("matnil");
        oracle.add("crimson11");
        oracle.add("mkouba");
        oracle.add("ehsavoie");
        oracle.add("jicken");

        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");

        final Form<Principal> form = new Form<Principal>(Principal.class);
        SuggestBoxItem nameItem = new SuggestBoxItem("name", "Name", true);
        nameItem.setOracle(oracle);
        TextBoxItem realmItem = new TextBoxItem("realm", "Realm", false);
        form.setFields(nameItem, realmItem);
        // TODO add help panel
        layout.add(form.asWidget());

        DialogueOptions options = new DialogueOptions(
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        FormValidation validation = form.validate();
                        if (!validation.hasErrors()) {
                            Principal principal = form.getUpdatedEntity();
                            principal.setType(principalType);
                            presenter.onAdd(role, roleAssignment, principal);
                        }
                    }
                },
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        presenter.closeDialog();
                    }
                }
        );

        return new WindowContentBuilder(layout, options).build();
    }
}
