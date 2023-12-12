package com.nvlad.yii2support.migrations.ui.toolWindow;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.UIUtil;
import com.nvlad.yii2support.migrations.actions.*;
import com.nvlad.yii2support.migrations.services.MigrationService;
import com.nvlad.yii2support.migrations.util.TreeUtil;
import com.nvlad.yii2support.utils.Yii2SupportSettings;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;

public class MigrationPanel extends SimpleToolWindowPanel {
    private final Project myProject;
    private CheckboxTree myTree;
//    private final Map<MigrateCommand, Collection<Migration>> myMigrationMap;

    public MigrationPanel(Project project, ToolWindow toolWindow) {
        super(false);

        myProject = project;

        initContent();
        initToolBar();
//        initActivationListener(toolWindow);

//        myMigrationMap = MigrationService.getInstance(myProject).getMigrationCommandMap();

//        DumbService.getInstance(project).runWhenSmart(() -> {
//            boolean newestFirst = Yii2SupportSettings.getInstance(myProject).newestFirst;
//            MigrationService service = MigrationService.getInstance(myProject);
//
//            service.refresh();
//            MigrationUtil.updateTree(myTree, service.getMigrationCommandMap(), newestFirst);
//        });
//
        toolWindow.hide(() -> {
        });
    }

    public JTree getTree() {
        return myTree;
    }

    public void updateTree() {
        boolean newestFirst = Yii2SupportSettings.getInstance(myProject).newestFirst;
        MigrationService service = MigrationService.getInstance(myProject);
        TreeUtil.updateTree(myTree, service.getMigrationCommandMap(), newestFirst);
    }

    //    public Map<MigrateCommand, Collection<Migration>> getMigrationMap() {
//        return myMigrationMap;
//    }

    private void initActivationListener(ToolWindow toolWindow) {
//            boolean newestFirst = Yii2SupportSettings.getInstance(myProject).newestFirst;
            MigrationService service = MigrationService.getInstance(myProject);

            service.sync();
            updateTree();

            DefaultTreeModel treeModel = ((DefaultTreeModel) myTree.getModel());
            treeModel.nodeStructureChanged((TreeNode) treeModel.getRoot());

    }

    private void initContent() {
        MigrationTreeCellRenderer renderer = new MigrationTreeCellRenderer();
        CheckedTreeNode myRootNode = new CheckedTreeNode();
//        myRootNode.add(new DefaultMutableTreeNode("Init"));

        myTree = new CheckboxTree(renderer, myRootNode);
        myTree.addMouseListener(new MigrationsMouseListener());
        myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        JBScrollPane scrollPane = new JBScrollPane(myTree);
        UIUtil.removeScrollBorder(scrollPane);
        setContent(scrollPane);
    }


    private void initToolBar() {
        ActionToolbar toolbar = createToolbar();
        setToolbar(toolbar.getComponent());
    }

    private ActionToolbar createToolbar() {
        DefaultActionGroup group = new DefaultActionGroup();
        try {
            group.add(createAction(RefreshAction.class));
            group.add(new Separator());
            group.add(createAction(MigrateUpAction.class));
            group.add(createAction(MigrateDownAction.class));
            group.add(createAction(MigrateRedoAction.class));
            group.add(new Separator());
            group.add(createAction(OrderAscAction.class));
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }

        return ActionManager.getInstance().createActionToolbar("Migrations", group, false);
    }

    private <T extends AnActionButton> T createAction(Class<T> clazz) throws IllegalAccessException, InstantiationException {
        T action = clazz.newInstance();
        action.setContextComponent(this);
        return action;
    }
}
