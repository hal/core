package org.jboss.as.console.client.domain.profiles;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.domain.events.ProfileSelectionEvent;
import org.jboss.as.console.client.domain.model.ProfileRecord;
import org.jboss.as.console.client.plugins.SubsystemExtensionMetaData;
import org.jboss.as.console.client.plugins.SubsystemRegistry;
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.tree.GroupItem;
import org.jboss.ballroom.client.layout.LHSHighlightEvent;
import org.jboss.ballroom.client.layout.LHSNavTreeItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Heiko Braun
 * @since 09/01/15
 */
public class ColumnProfileView extends SuspendableViewImpl
        implements ProfileMgmtPresenter.MyView, LHSHighlightEvent.NavItemSelectionHandler {

    private final FinderColumn<ProfileRecord> profiles;
    private final FinderColumn<SubsystemLink> subsystems;

    private SplitLayoutPanel layout;
    private LayoutPanel contentCanvas;

    interface Template extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\">{1}</div>")
        SafeHtml item(String cssClass, String title);
    }

    private static final Template TEMPLATE = GWT.create(Template.class);


    public ColumnProfileView() {
        super();

        contentCanvas = new LayoutPanel();
        layout = new SplitLayoutPanel(2);
        profiles = new FinderColumn<ProfileRecord>(
                "Profiles",
                new FinderColumn.Display<ProfileRecord>() {
                    @Override
                    public boolean isFolder(ProfileRecord data) {
                        return true;
                    }

                    @Override
                    public SafeHtml render(String baseCss, ProfileRecord data) {
                        return TEMPLATE.item(baseCss, data.getName());
                    }

                    @Override
                    public String rowCss(ProfileRecord data) {
                        return "";
                    }
                },
                new ProvidesKey<ProfileRecord>() {
                    @Override
                    public Object getKey(ProfileRecord item) {
                        return item.getName();
                    }
                });

        subsystems = new FinderColumn<SubsystemLink>(
                "Subsystems",
                new FinderColumn.Display<SubsystemLink>() {

                    @Override
                    public boolean isFolder(SubsystemLink data) {
                        return data.isFolder();
                    }

                    @Override
                    public SafeHtml render(String baseCss, SubsystemLink data) {
                        return TEMPLATE.item(baseCss, data.getTitle());
                    }

                    @Override
                    public String rowCss(SubsystemLink data) {
                        return "";
                    }
                },
                new ProvidesKey<SubsystemLink>() {
                    @Override
                    public Object getKey(SubsystemLink item) {
                        return item.getToken();
                    }
                });

        layout.addWest(profiles.asWidget(), 217);
        layout.addWest(subsystems.asWidget(), 217);
        layout.add(contentCanvas);

        // event handler

        profiles.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {

                if(profiles.hasSelectedItem()) {

                    final ProfileRecord selectedProfile = profiles.getSelectedItem();

                    Scheduler.get().scheduleDeferred(
                            new Scheduler.ScheduledCommand() {
                                @Override
                                public void execute() {
                                    Console.getEventBus().fireEvent(
                                            new ProfileSelectionEvent(selectedProfile.getName())
                                    );
                                }
                            }
                    );

                }
            }
        });

        subsystems.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {

                if(subsystems.hasSelectedItem()) {

                    final SubsystemLink selectedSubsystem = subsystems.getSelectedItem();

                    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                        public void execute() {

                            Console.MODULES.getPlaceManager().revealPlace(
                                    new PlaceRequest.Builder().nameToken(selectedSubsystem.getToken()).build()
                            );
                        }
                    });
                }
            }
        });
    }

    @Override
    public Widget createWidget() {
        return layout.asWidget();
    }

    @Override
    public void setInSlot(Object slot, IsWidget content) {

        if (slot == ProfileMgmtPresenter.TYPE_MainContent) {
            if(content!=null)
                setContent(content);
        }
    }

    private void setContent(IsWidget newContent) {
        contentCanvas.clear();
        contentCanvas.add(newContent);
    }

    @Override
    public void setProfiles(List<ProfileRecord> profileRecords) {
        profiles.updateFrom(profileRecords, true);
    }

    @Override
    public void setSubsystems(List<SubsystemRecord> subsystemRecords)
    {
        subsystems.updateFrom(matchSubsystems(subsystemRecords));
    }

    @Override
    public void setPreselection(String preselection) {

    }

    @Override
    public void onSelectedNavTree(LHSHighlightEvent event) {
        subsystems.selectByKey(event.getToken());
    }

    class SubsystemLink
    {
        String title;
        String token;
        private final boolean isFolder;

        public SubsystemLink(String title, String token, boolean isFolder) {
            this.title = title;
            this.token = token;
            this.isFolder = isFolder;
        }

        public String getTitle() {
            return title;
        }

        public String getToken() {
            return token;
        }

        public boolean isFolder() {
            return isFolder;
        }
    }

    private List<SubsystemLink> matchSubsystems(List<SubsystemRecord> subsystems)
    {

        List<SubsystemLink> matches = new ArrayList<>();

        SubsystemRegistry registry = Console.getSubsystemRegistry();

        Map<String, List<SubsystemExtensionMetaData>> grouped = new HashMap<String, List<SubsystemExtensionMetaData>>();
        List<String> groupNames = new ArrayList<String>();
        for(SubsystemExtensionMetaData ext : registry.getExtensions())
        {
            if(!grouped.containsKey(ext.getGroup()))
            {
                groupNames.add(ext.getGroup());
                grouped.put(ext.getGroup(), new ArrayList<SubsystemExtensionMetaData>());
            }

            grouped.get(ext.getGroup()).add(ext);
        }

        int includedSubsystems = 0;


        Collections.sort(groupNames);

        // build groups first
        for(String groupName : groupNames)
        {
            List<SubsystemExtensionMetaData> items = grouped.get(groupName);

            final GroupItem groupTreeItem = new GroupItem(groupName);

            for(SubsystemExtensionMetaData candidate : items)
            {
                for(SubsystemRecord actual: subsystems)
                {
                    if(actual.getKey().equals(candidate.getKey()))
                    {
                        includedSubsystems++;

                        final LHSNavTreeItem link = new LHSNavTreeItem(candidate.getName(), candidate.getToken());
                        link.setKey(candidate.getKey());
                        link.getElement().setAttribute("title", candidate.getName()+" "+
                                actual.getMajor()+"."+
                                actual.getMinor()+"."+
                                actual.getMicro());

                            /*if(compatibleVersion(actual, candidate)) {
                                groupTreeItem.addItem(link);
                            }*/

                        matches.add(
                                new SubsystemLink(candidate.getName(), candidate.getToken(), false)
                        );

                    }
                }
            }


            // skip empty groups
               /* if(groupTreeItem.getChildCount()>0)
                    matches.add(new SubsystemLink());*/

        }

            /*// fallback in case no manageable subsystems exist
            if(includedSubsystems==0)
            {
                HTML explanation = new HTML("No manageable subsystems exist.");
                explanation.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        displaySubsystemHelp(subsysTree);
                    }
                });
                subsysTree.addItem(new TreeItem(explanation));
            }*/

        return matches;
    }

}
