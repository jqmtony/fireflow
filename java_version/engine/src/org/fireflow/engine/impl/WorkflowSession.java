/**
 * Copyright 2007-2008 非也
 * All rights reserved. 
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation。
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses. *
 */
package org.fireflow.engine.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.fireflow.engine.EngineException;
import org.fireflow.engine.IWorkflowSessionCallback;
import org.fireflow.engine.IProcessInstance;
import org.fireflow.engine.IRuntimeContextAware;
import org.fireflow.engine.IWorkItem;
import org.fireflow.engine.ITaskInstance;
import org.fireflow.engine.IWorkflowSession;
import org.fireflow.engine.IWorkflowSessionAware;
import org.fireflow.engine.RuntimeContext;
import org.fireflow.engine.definition.WorkflowDefinition;
import org.fireflow.engine.taskinstance.DynamicAssignmentHandler;
import org.fireflow.engine.persistence.IPersistenceService;
import org.fireflow.kernel.KernelException;
import org.fireflow.model.DataField;
import org.fireflow.model.WorkflowProcess;

/**
 * @author chennieyun
 * 
 */
public class WorkflowSession implements IWorkflowSession, IRuntimeContextAware {

    protected RuntimeContext runtimeContext = null;
    protected DynamicAssignmentHandler dynamicAssignmentHandler = null;
    protected boolean inWithdrawOrRejectOperation = false;

    public void setRuntimeContext(RuntimeContext ctx) {
        this.runtimeContext = ctx;
    }

    public WorkflowSession(RuntimeContext ctx) {
        this.runtimeContext = ctx;
    }

    public void setCurrentDynamicAssignmentHandler(DynamicAssignmentHandler handler) {
        this.dynamicAssignmentHandler = handler;
    }

    public DynamicAssignmentHandler consumeCurrentDynamicAssignmentHandler() {
        DynamicAssignmentHandler handler = this.dynamicAssignmentHandler;
        this.dynamicAssignmentHandler = null;
        return handler;
    }

    public IProcessInstance createProcessInstance(String workflowProcessName)throws EngineException,KernelException{
        return _createProcessInstance(workflowProcessName,null,null,null);
    }
	public IProcessInstance createProcessInstance(String workflowProcessName,ITaskInstance parentTaskInstance)
			throws EngineException,KernelException{
            return _createProcessInstance(workflowProcessName,parentTaskInstance.getId(),parentTaskInstance.getProcessInstanceId(),parentTaskInstance.getId());
    }

    protected IProcessInstance _createProcessInstance(String workflowProcessId,final String creatorId,final String parentProcessInstanceId,
            final String parentTaskInstanceId)throws EngineException,KernelException{
        // TODO Auto-generated method stub
        final String wfprocessId = workflowProcessId;
        return (IProcessInstance) this.execute(new IWorkflowSessionCallback() {

            public Object doInWorkflowSession(RuntimeContext ctx)
                    throws EngineException, KernelException {

                WorkflowDefinition workflowDef = ctx.getDefinitionService().getTheLatestVersionOfWorkflowDefinition(wfprocessId);
                WorkflowProcess wfProcess = null;

                wfProcess = workflowDef.getWorkflowProcess();

                if (wfProcess == null) {
                    throw new RuntimeException("Workflow process NOT found,id=[" + wfprocessId + "]");
                }

                ProcessInstance processInstance = new ProcessInstance();
                processInstance.setCreatorId(creatorId);
                processInstance.setProcessId(wfProcess.getId());
                processInstance.setVersion(workflowDef.getVersion());
                processInstance.setDisplayName(wfProcess.getDisplayName());
                processInstance.setName(wfProcess.getName());
                processInstance.setState(IProcessInstance.INITIALIZED);
                processInstance.setCreatedTime(ctx.getCalendarService().getSysDate());
                processInstance.setParentProcessInstanceId(parentProcessInstanceId);
                processInstance.setParentTaskInstanceId(parentTaskInstanceId);

                //初始化流程变量
                List datafields = wfProcess.getDataFields();
                for (int i = 0; datafields != null && i < datafields.size(); i++) {
                    DataField df = (DataField) datafields.get(i);
                    if (df.getDataType().equals(DataField.STRING)) {
                        if (df.getInitialValue() != null) {
                            processInstance.setProcessInstanceVariable(df.getName(), df.getInitialValue());
                        } else {
                            processInstance.setProcessInstanceVariable(df.getName(), "");
                        }
                    } else if (df.getDataType().equals(DataField.INTEGER)) {
                        if (df.getInitialValue() != null) {
                            try {
                                Integer intValue = new Integer(df.getInitialValue());
                                processInstance.setProcessInstanceVariable(df.getName(), intValue);
                            } catch (Exception e) {
                            }
                        } else {
                            processInstance.setProcessInstanceVariable(df.getName(), new Integer(0));
                        }
                    } else if (df.getDataType().equals(DataField.LONG)) {
                        if (df.getInitialValue() != null) {
                            try {
                                Long longValue = new Long(df.getInitialValue());
                                processInstance.setProcessInstanceVariable(df.getName(), longValue);
                            } catch (Exception e) {
                            }
                        } else {
                            processInstance.setProcessInstanceVariable(df.getName(), new Long(0));
                        }
                    } else if (df.getDataType().equals(DataField.FLOAT)) {
                        if (df.getInitialValue() != null) {
                            Float floatValue = new Float(df.getInitialValue());
                            processInstance.setProcessInstanceVariable(df.getName(), floatValue);
                        } else {
                            processInstance.setProcessInstanceVariable(df.getName(), new Float(0));
                        }
                    } else if (df.getDataType().equals(DataField.DOUBLE)) {
                        if (df.getInitialValue() != null) {
                            Double doubleValue = new Double(df.getInitialValue());
                            processInstance.setProcessInstanceVariable(df.getName(), doubleValue);
                        } else {
                            processInstance.setProcessInstanceVariable(df.getName(), new Double(0));
                        }
                    } else if (df.getDataType().equals(DataField.BOOLEAN)) {
                        if (df.getInitialValue() != null) {
                            Boolean booleanValue = new Boolean(df.getInitialValue());
                            processInstance.setProcessInstanceVariable(df.getName(), booleanValue);
                        } else {
                            processInstance.setProcessInstanceVariable(df.getName(), Boolean.FALSE);
                        }
                    } else if (df.getDataType().equals(DataField.DATETIME)) {
                        //TODO 需要完善一下
                        if (df.getInitialValue() != null && df.getDataPattern() != null) {
                            try {
                                SimpleDateFormat dFormat = new SimpleDateFormat(df.getDataPattern());
                                Date dateTmp = dFormat.parse(df.getInitialValue());
                                processInstance.setProcessInstanceVariable(df.getName(), dateTmp);
                            } catch (Exception e) {
                                processInstance.setProcessInstanceVariable(df.getName(), null);
                                e.printStackTrace();
                            }
                        } else {
                            processInstance.setProcessInstanceVariable(df.getName(), null);
                        }
                    }
                }


                ctx.getPersistenceService().saveOrUpdateProcessInstance(processInstance);


                return processInstance;
            }
        });
    }

    public IProcessInstance createProcessInstance(String workflowProcessId,final String creatorId)
            throws EngineException, KernelException {
            return _createProcessInstance(workflowProcessId,creatorId,null,null);
    }

    public IWorkItem findWorkItemById(String id) {
        final String workItemId = id;
        try {
            return (IWorkItem) this.execute(new IWorkflowSessionCallback() {

                public Object doInWorkflowSession(RuntimeContext ctx)
                        throws EngineException, KernelException {
                    IPersistenceService persistenceService = ctx.getPersistenceService();

                    return persistenceService.findWorkItemById(workItemId);
                }
            });
        } catch (EngineException ex) {
            ex.printStackTrace();
            return null;
        } catch (KernelException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * WorkflowSession采用了模板模式，以便将来有需要时可以在本方法中进行扩展。
     * @param callback
     * @return
     * @throws org.fireflow.engine.EngineException
     * @throws org.fireflow.kenel.KenelException
     */
    public Object execute(IWorkflowSessionCallback callback)
            throws EngineException, KernelException {
        Object result = callback.doInWorkflowSession(runtimeContext);
        if (result != null) {
            if (result instanceof IRuntimeContextAware) {
                ((IRuntimeContextAware) result).setRuntimeContext(this.runtimeContext);
            }
            if (result instanceof IWorkflowSessionAware) {
                ((IWorkflowSessionAware) result).setCurrentWorkflowSession(this);
            }

            if (result instanceof List) {
                List l = (List) result;
                for (int i = 0; i < l.size(); i++) {
                    Object item = l.get(i);
                    if (item instanceof IRuntimeContextAware) {
                        ((IRuntimeContextAware) item).setRuntimeContext(runtimeContext);
                        if (item instanceof IWorkflowSessionAware) {
                            ((IWorkflowSessionAware) item).setCurrentWorkflowSession(this);
                        }
                    } else {
                        break;
                    }
                }
            }
        }
        return result;
    }

    public RuntimeContext getRuntimeContext() {
        return runtimeContext;
    }

    public ITaskInstance findTaskInstanceById(String id) {
        final String taskInstanceId = id;
        try {
            return (ITaskInstance) this.execute(new IWorkflowSessionCallback() {

                public Object doInWorkflowSession(RuntimeContext ctx)
                        throws EngineException, KernelException {
                    IPersistenceService persistenceService = ctx.getPersistenceService();

                    return persistenceService.findTaskInstanceById(taskInstanceId);
                }
            });
        } catch (EngineException ex) {
            ex.printStackTrace();
            return null;
        } catch (KernelException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public List<IWorkItem> findMyTodoWorkItems(final String actorId) {
        List result = null;
        try {
            result = (List) this.execute(new IWorkflowSessionCallback() {

                public Object doInWorkflowSession(RuntimeContext ctx) throws EngineException, KernelException {
                    return ctx.getPersistenceService().findTodoWorkItems(actorId);
                }
            });
        } catch (EngineException ex) {
            Logger.getLogger(WorkflowSession.class.getName()).log(Level.SEVERE, null, ex);
        } catch (KernelException ex) {
            Logger.getLogger(WorkflowSession.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    public List<IWorkItem> findMyTodoWorkItems(final String actorId, final String processInstanceId) {
        List result = null;
        try {
            result = (List) this.execute(new IWorkflowSessionCallback() {

                public Object doInWorkflowSession(RuntimeContext ctx) throws EngineException, KernelException {
                    return ctx.getPersistenceService().findTodoWorkItems(actorId, processInstanceId);
                }
            });
        } catch (EngineException ex) {
            Logger.getLogger(WorkflowSession.class.getName()).log(Level.SEVERE, null, ex);
        } catch (KernelException ex) {
            Logger.getLogger(WorkflowSession.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    public List<IWorkItem> findMyTodoWorkItems(final String actorId, final String processId, final String taskId) {
        List result = null;
        try {
            result = (List) this.execute(new IWorkflowSessionCallback() {

                public Object doInWorkflowSession(RuntimeContext ctx) throws EngineException, KernelException {
                    return ctx.getPersistenceService().findTodoWorkItems(actorId, processId, taskId);
                }
            });
        } catch (EngineException ex) {
            Logger.getLogger(WorkflowSession.class.getName()).log(Level.SEVERE, null, ex);
        } catch (KernelException ex) {
            Logger.getLogger(WorkflowSession.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    public void setWithdrawOrRejectOperationFlag(boolean b){
        this.inWithdrawOrRejectOperation = b;
    }

    public boolean isInWithdrawOrRejectOperation(){
        return this.inWithdrawOrRejectOperation;
    }
}
