/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.api.db.hibernate;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.hibernate.type.Type;
import org.junit.Assert;
import org.junit.Test;
import org.openmrs.Auditable;
import org.openmrs.ConceptNumeric;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.scheduler.Task;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.openmrs.scheduler.timer.TimerSchedulerTask;
import org.openmrs.test.BaseContextSensitiveTest;

public class AuditableInterceptorTest extends BaseContextSensitiveTest {
	
	/**
	 * @see AuditableInterceptor#onFlushDirty(Object,Serializable,Object[],Object[],String[],Type[])
	 * @verifies return false for non Auditable objects
	 */
	@Test
	public void onFlushDirty_shouldReturnFalseForNonAuditableObjects() throws Exception {
		AuditableInterceptor interceptor = new AuditableInterceptor();
		
		Object o = new Object();
		
		boolean returnValue = interceptor.onFlushDirty(o, null, null, null, null, null);
		
		Assert.assertFalse("false should have been returned because we didn't pass in an Auditable or OpenmrsObject",
		    returnValue);
	}
	
	/**
	 * @see AuditableInterceptor#onFlushDirty(Object,Serializable,Object[],Object[],String[],Type[])
	 * @verifies set the changedBy field
	 */
	@Test
	public void onFlushDirty_shouldSetTheChangedByField() throws Exception {
		AuditableInterceptor interceptor = new AuditableInterceptor();
		
		User u = new User();
		
		// sanity check
		Assert.assertTrue(u instanceof Auditable);
		
		String[] propertyNames = new String[] { "changedBy", "dateChanged" };
		Object[] currentState = new Object[] { "", null };
		Object[] previousState = new Object[] { "", null };
		
		interceptor.onFlushDirty(u, null, currentState, previousState, propertyNames, null);
		
		Assert.assertNotNull(currentState[0]);
	}
	
	/**
	 * @see AuditableInterceptor#onFlushDirty(Object,Serializable,Object[],Object[],String[],Type[])
	 * @verifies set the dateChanged field
	 */
	@Test
	public void onFlushDirty_shouldSetTheDateChangedField() throws Exception {
		AuditableInterceptor interceptor = new AuditableInterceptor();
		
		User u = new User();
		
		// sanity check
		Assert.assertTrue(u instanceof Auditable);
		
		String[] propertyNames = new String[] { "changedBy", "dateChanged" };
		Object[] currentState = new Object[] { "", null };
		Object[] previousState = new Object[] { "", null };
		
		interceptor.onFlushDirty(u, null, currentState, previousState, propertyNames, null);
		
		Assert.assertNotNull(currentState[1]);
	}
	
	/**
	 * @see AuditableInterceptor#onFlushDirty(Object,Serializable,Object[],Object[],String[],Type[])
	 * @verifies set the dateChanged field
	 */
	@Test
	public void onFlushDirty_shouldNotFailWithNullPreviousState() throws Exception {
		AuditableInterceptor interceptor = new AuditableInterceptor();
		
		User u = new User();
		
		// sanity check
		Assert.assertTrue(u instanceof Auditable);
		
		String[] propertyNames = new String[] { "changedBy", "dateChanged" };
		Object[] currentState = new Object[] { "", null };
		
		interceptor.onFlushDirty(u, null, currentState, null, propertyNames, null);
	}
	
	/**
	 * This test makes sure that the AuditableInterceptor is registered on the session
	 * 
	 * @see AuditableInterceptor#onFlushDirty(Object,Serializable,Object[],Object[],String[],Type[])
	 * @verifies be called when saving an Auditable
	 */
	@Test
	public void onFlushDirty_shouldBeCalledWhenSavingAnAuditable() throws Exception {
		User u = Context.getUserService().getUser(1);
		
		u.setUsername("asdf");
		
		Date beforeDate = u.getDateChanged();
		
		Context.getUserService().saveUser(u, null);
		
		Date afterDate = u.getDateChanged();
		
		Assert.assertNotSame(beforeDate, afterDate);
	}
	
	/**
	 * @see {@link AuditableInterceptor#onFlushDirty(Object,Serializable,null,null,null,null)}
	 * @verifies should not fail when the daemon user modifies something
	 */
	@Test
	public void onFlushDirty_shouldNotFailWhenTheDaemonUserModifiesSomething() throws Throwable {
		new AsDaemonTask(new AbstractTask() {
			
			@Override
			public void execute() {
				ConceptNumeric weight = Context.getConceptService().getConceptNumeric(5089);
				Date dateChangedBefore = weight.getDateChanged();
				weight.setHiAbsolute(75d);
				Context.getConceptService().saveConcept(weight);
				Assert.assertNotSame(dateChangedBefore, weight.getDateChanged());
			}
		}).runTheTask();
	}
	
	private class AsDaemonTask extends TimerSchedulerTask {
		
		private Task task;
		
		public AsDaemonTask(Task task) {
			super(task);
			this.task = task;
		}
		
		public boolean runTheTask() throws Throwable {
			Daemon.executeScheduledTask(this.task);
			return true;
		}
	}
	
	/**
	 * @see AuditableInterceptor#onSave(Object,Serializable,Object[],String[],Type[])
	 * @verifies return true if dateCreated was null
	 */
	@Test
	public void onSave_shouldReturnTrueIfDateCreatedWasNull() throws Exception {
		AuditableInterceptor interceptor = new AuditableInterceptor();
		
		User u = new User();
		
		String[] propertyNames = new String[] { "creator", "dateCreated", "changedBy", "dateChanged" };
		Object[] currentState = new Object[] { 0, null, 0, new Date() };
		
		boolean result = interceptor.onSave(u, 0, currentState, propertyNames, null);
		Assert.assertTrue(result);
	}
	
	/**
	 * @see AuditableInterceptor#onSave(Object,Serializable,Object[],String[],Type[])
	 * @verifies return true if creator was null
	 */
	@Test
	public void onSave_shouldReturnTrueIfCreatorWasNull() throws Exception {
		AuditableInterceptor interceptor = new AuditableInterceptor();
		
		User u = new User();
		
		String[] propertyNames = new String[] { "creator", "dateCreated", "changedBy", "dateChanged" };
		Object[] currentState = new Object[] { null, new Date(), 0, new Date() };
		
		boolean result = interceptor.onSave(u, 0, currentState, propertyNames, null);
		Assert.assertTrue(result);
	}
	
	/**
	 * @see AuditableInterceptor#onSave(Object,Serializable,Object[],String[],Type[])
	 * @verifies return true if changedBy was null
	 */
	@Test
	public void onSave_shouldReturnTrueIfChangedByWasNull() throws Exception {
		AuditableInterceptor interceptor = new AuditableInterceptor();
		
		User u = new User();
		
		String[] propertyNames = new String[] { "creator", "dateCreated", "changedBy", "dateChanged" };
		Object[] currentState = new Object[] { 0, new Date(), null, new Date() };
		
		boolean result = interceptor.onSave(u, 0, currentState, propertyNames, null);
		Assert.assertTrue(result);
	}
	
	/**
	 * @see AuditableInterceptor#onSave(Object,Serializable,Object[],String[],Type[])
	 * @verifies return true if dateChanged was null
	 */
	@Test
	public void onSave_shouldReturnTrueIfDateChangedWasNull() throws Exception {
		AuditableInterceptor interceptor = new AuditableInterceptor();
		
		User u = new User();
		
		String[] propertyNames = new String[] { "creator", "dateCreated", "changedBy", "dateChanged" };
		Object[] currentState = new Object[] { 0, new Date(), 0, null };
		
		boolean result = interceptor.onSave(u, 0, currentState, propertyNames, null);
		Assert.assertTrue(result);
	}
	
	/**
	 * @see AuditableInterceptor#onSave(Object,Serializable,Object[],String[],Type[])
	 * @verifies return true if dateChanged was null
	 */
	@Test
	public void onSave_shouldSetDateChangedToDateCreatedIfDateChangedWasNull() throws Exception {
		AuditableInterceptor interceptor = new AuditableInterceptor();
		
		User u = new User();
		
		String[] propertyNames = new String[] { "creator", "dateCreated", "changedBy", "dateChanged" };
		Date dateCreated = new SimpleDateFormat("yyyy/mm/dd").parse("1999/12/26");
		Object[] currentState = new Object[] { 0, dateCreated, 0, null };
		
		interceptor.onSave(u, 0, currentState, propertyNames, null);
		
		Assert.assertEquals(dateCreated, currentState[3]);
	}
	
	/**
	 * @see AuditableInterceptor#onSave(Object,Serializable,Object[],String[],Type[])
	 * @verifies return false if dateCreated and creator was not null
	 */
	@Test
	public void onSave_shouldReturnFalseIfDateCreatedAndCreatorWasNotNull() throws Exception {
		AuditableInterceptor interceptor = new AuditableInterceptor();
		
		User u = new User();
		
		String[] propertyNames = new String[] { "creator", "dateCreated", "changedBy", "dateChanged" };
		Object[] currentState = new Object[] { 0, new Date(), 0, new Date() };
		
		boolean result = interceptor.onSave(u, 0, currentState, propertyNames, null);
		Assert.assertFalse(result);
	}
	
	/**
	 * @see AuditableInterceptor#onSave(Object,Serializable,Object[],String[],Type[])
	 * @verifies be called when saving OpenmrsObject
	 */
	@Test
	public void onSave_shouldBeCalledWhenSavingOpenmrsObject() throws Exception {
		User u = new User();
		
		u.setSystemId("user");
		u.setPerson(Context.getPersonService().getPerson(1));
		
		Context.getUserService().saveUser(u, "Admin123");
		
		Assert.assertSame(Context.getAuthenticatedUser(), u.getCreator());
		Assert.assertNotNull(u.getDateCreated());
	}
	
}
