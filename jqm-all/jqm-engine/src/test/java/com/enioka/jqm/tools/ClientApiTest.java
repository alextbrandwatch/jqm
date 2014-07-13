/**
 * Copyright © 2013 enioka. All rights reserved
 * Authors: Marc-Antoine GOUILLART (marc-antoine.gouillart@enioka.com)
 *          Pierre COPPEE (pierre.coppee@enioka.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.enioka.jqm.tools;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.junit.Assert;
import org.junit.Test;

import com.enioka.jqm.api.JobRequest;
import com.enioka.jqm.api.JqmClientFactory;
import com.enioka.jqm.jpamodel.History;
import com.enioka.jqm.jpamodel.JobDef;
import com.enioka.jqm.jpamodel.JobDefParameter;
import com.enioka.jqm.jpamodel.JobInstance;
import com.enioka.jqm.jpamodel.Message;
import com.enioka.jqm.jpamodel.State;
import com.enioka.jqm.test.helpers.CreationTools;
import com.enioka.jqm.test.helpers.TestHelpers;

public class ClientApiTest extends JqmBaseTest
{
    @Test
    public void testRestartJob() throws Exception
    {
        jqmlogger.debug("**********************************************************");
        jqmlogger.debug("Starting test testRestartJob");
        EntityManager em = Helpers.getNewEm();
        TestHelpers.cleanup(em);
        TestHelpers.createLocalNode(em);

        ArrayList<JobDefParameter> jdargs = new ArrayList<JobDefParameter>();
        JobDefParameter jdp = CreationTools.createJobDefParameter("arg", "POUPETTE", em);
        jdargs.add(jdp);

        JobDef jdDemoMaven = CreationTools.createJobDef(null, true, "App", jdargs, "jqm-tests/jqm-test-datetimemaven/target/test.jar",
                TestHelpers.qVip, 42, "MarsuApplication", null, "Franquin", "ModuleMachin", "other", "other", true, em);

        JobRequest j = new JobRequest("MarsuApplication", "MAG");

        int i = JqmClientFactory.getClient().enqueue(j);

        JqmEngine engine1 = new JqmEngine();
        engine1.start("localhost");

        TestHelpers.waitFor(1, 10000, em);
        JqmClientFactory.getClient().enqueueFromHistory(i);
        TestHelpers.waitFor(2, 10000, em);
        engine1.stop();

        TypedQuery<History> query = em.createQuery("SELECT j FROM History j ORDER BY j.enqueueDate ASC", History.class);
        ArrayList<History> res = (ArrayList<History>) query.getResultList();

        Assert.assertEquals(2, res.size());
        Assert.assertEquals(jdDemoMaven.getId(), res.get(0).getJd().getId());
        Assert.assertEquals(jdDemoMaven.getId(), res.get(1).getJd().getId());

    }

    @Test
    public void testHistoryFields() throws Exception
    {
        jqmlogger.debug("**********************************************************");
        jqmlogger.debug("Starting test testHistoryFields");
        EntityManager em = Helpers.getNewEm();
        TestHelpers.cleanup(em);
        TestHelpers.createLocalNode(em);

        ArrayList<JobDefParameter> jdargs = new ArrayList<JobDefParameter>();
        JobDefParameter jdp = CreationTools.createJobDefParameter("arg", "POUPETTE", em);
        jdargs.add(jdp);

        @SuppressWarnings("unused")
        JobDef jdDemoMaven = CreationTools.createJobDef(null, true, "App", jdargs, "jqm-tests/jqm-test-datetimemaven/target/test.jar",
                TestHelpers.qVip, 42, "MarsuApplication", null, "Franquin", "ModuleMachin", "other", "other", true, em);

        JobRequest j = new JobRequest("MarsuApplication", "MAG");
        j.setSessionID("session42");

        int i = JqmClientFactory.getClient().enqueue(j);

        JqmEngine engine1 = new JqmEngine();
        engine1.start("localhost");
        TestHelpers.waitFor(1, 10000, em);
        engine1.stop();

        History h = em.createQuery("SELECT h FROM History h WHERE h.id = :i", History.class).setParameter("i", i).getSingleResult();

        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        jqmlogger.debug("enqueueDate: " + df.format(h.getEnqueueDate().getTime()));
        jqmlogger.debug("ExecutionDate: " + df.format(h.getExecutionDate().getTime()));
        jqmlogger.debug("EndDate: " + df.format(h.getEndDate().getTime()));

        Assert.assertTrue(h.getEnqueueDate() != null);
        Assert.assertTrue(h.getUserName() != null);
        Assert.assertTrue(h.getEndDate() != null);
        Assert.assertTrue(h.getExecutionDate() != null);
        Assert.assertTrue(h.getSessionId() != null);
    }

    @Test
    public void testKillJob() throws Exception
    {
        jqmlogger.debug("**********************************************************");
        jqmlogger.debug("Starting test testKillJob");
        EntityManager em = Helpers.getNewEm();
        TestHelpers.cleanup(em);
        TestHelpers.createLocalNode(em);

        ArrayList<JobDefParameter> jdargs = new ArrayList<JobDefParameter>();
        JobDefParameter jdp = CreationTools.createJobDefParameter("arg", "POUPETTE", em);
        jdargs.add(jdp);

        @SuppressWarnings("unused")
        JobDef jdDemoMaven = CreationTools.createJobDef(null, true, "App", jdargs, "jqm-tests/jqm-test-kill/target/test.jar",
                TestHelpers.qVip, 42, "MarsuApplication", null, "Franquin", "ModuleMachin", "other", "other", false, em);

        JobRequest j = new JobRequest("MarsuApplication", "MAG");

        int i = JqmClientFactory.getClient().enqueue(j);
        // JqmClientFactory.getClient().enqueue(j);

        JqmEngine engine1 = new JqmEngine();
        engine1.start("localhost");
        Thread.sleep(3000);

        JqmClientFactory.getClient().killJob(i);

        Thread.sleep(3000);

        engine1.stop();

        TypedQuery<History> query = Helpers.getNewEm().createQuery("SELECT j FROM History j", History.class);
        ArrayList<History> res = (ArrayList<History>) query.getResultList();

        Assert.assertEquals(1, res.size());
        Assert.assertEquals(State.KILLED, res.get(0).getState());
    }

    @Test
    public void testGetMsg() throws Exception
    {
        jqmlogger.debug("**********************************************************");
        jqmlogger.debug("Starting test testGetMsg");
        EntityManager em = Helpers.getNewEm();
        TestHelpers.cleanup(em);
        TestHelpers.createLocalNode(em);
        boolean success = false;
        boolean success2 = false;
        boolean success3 = false;

        ArrayList<JobDefParameter> jdargs = new ArrayList<JobDefParameter>();
        JobDefParameter jdp = CreationTools.createJobDefParameter("arg", "POUPETTE", em);
        jdargs.add(jdp);

        @SuppressWarnings("unused")
        JobDef jdDemoMaven = CreationTools.createJobDef(null, true, "App", jdargs, "jqm-tests/jqm-test-sendmsg/target/test.jar",
                TestHelpers.qVip, 42, "Marsu-Application", null, "Franquin", "ModuleMachin", "other", "other", true, em);

        JobRequest j = new JobRequest("Marsu-Application", "MAG");

        int i = JqmClientFactory.getClient().enqueue(j);

        JqmEngine engine1 = new JqmEngine();
        engine1.start("localhost");
        TestHelpers.waitFor(1, 10000, em);
        engine1.stop();

        TypedQuery<History> query = em.createQuery("SELECT j FROM History j ORDER BY j.enqueueDate ASC", History.class);
        ArrayList<History> res = (ArrayList<History>) query.getResultList();

        ArrayList<String> ress = (ArrayList<String>) JqmClientFactory.getClient().getJobMessages(i);

        @SuppressWarnings("unused")
        ArrayList<Message> m = (ArrayList<Message>) em.createQuery("SELECT m FROM Message m WHERE m.history.id = :i", Message.class)
                .setParameter("i", i).getResultList();

        Assert.assertEquals(1, res.size());
        Assert.assertEquals(State.ENDED, res.get(0).getState());

        for (int k = 0; k < ress.size(); k++)
        {
            if (ress.get(k).equals("Les marsus sont nos amis, il faut les aimer aussi!"))
            {
                success = true;
            }
            if (ress.get(k).equals("Les marsus sont nos amis, il faut les aimer aussi!2"))
            {
                success2 = true;
            }
            if (ress.get(k).equals("Les marsus sont nos amis, il faut les aimer aussi!3"))
            {
                success3 = true;
            }
        }

        Assert.assertEquals(true, success);
        Assert.assertEquals(true, success2);
        Assert.assertEquals(true, success3);
    }

    @Test
    public void testGetProgress() throws Exception
    {
        jqmlogger.debug("**********************************************************");
        jqmlogger.debug("Starting test testGetProgress");
        EntityManager em = Helpers.getNewEm();
        TestHelpers.cleanup(em);
        TestHelpers.createLocalNode(em);

        ArrayList<JobDefParameter> jdargs = new ArrayList<JobDefParameter>();
        JobDefParameter jdp = CreationTools.createJobDefParameter("arg", "POUPETTE", em);
        jdargs.add(jdp);

        @SuppressWarnings("unused")
        JobDef jdDemoMaven = CreationTools.createJobDef(null, true, "App", jdargs, "jqm-tests/jqm-test-sendprogress/target/test.jar",
                TestHelpers.qVip, 42, "MarsuApplication", null, "Franquin", "ModuleMachin", "other", "other", true, em);

        JobRequest j = new JobRequest("MarsuApplication", "MAG");

        int i = JqmClientFactory.getClient().enqueue(j);

        JqmEngine engine1 = new JqmEngine();
        engine1.start("localhost");
        TestHelpers.waitFor(1, 10000, em);
        engine1.stop();

        TypedQuery<History> query = em.createQuery("SELECT j FROM History j ORDER BY j.enqueueDate ASC", History.class);
        ArrayList<History> res = (ArrayList<History>) query.getResultList();

        Integer k = JqmClientFactory.getClient().getJobProgress(i);

        Assert.assertEquals(1, res.size());
        Assert.assertEquals(State.ENDED, res.get(0).getState());
        Assert.assertEquals((Integer) 50, k);
    }

    @Test
    public void testPause() throws Exception
    {
        jqmlogger.debug("**********************************************************");
        jqmlogger.debug("Starting test testPause");
        EntityManager em = Helpers.getNewEm();
        TestHelpers.cleanup(em);
        TestHelpers.createLocalNode(em);

        ArrayList<JobDefParameter> jdargs = new ArrayList<JobDefParameter>();
        JobDefParameter jdp = CreationTools.createJobDefParameter("arg", "POUPETTE", em);
        jdargs.add(jdp);

        @SuppressWarnings("unused")
        JobDef jdDemoMaven = CreationTools.createJobDef(null, true, "App", jdargs, "jqm-tests/jqm-test-datetimemaven/target/test.jar",
                TestHelpers.qVip, 42, "MarsuApplication", null, "Franquin", "ModuleMachin", "other", "other", false, em);

        JobRequest j = new JobRequest("MarsuApplication", "MAG");

        int i = JqmClientFactory.getClient().enqueue(j);
        @SuppressWarnings("unused")
        int ii = JqmClientFactory.getClient().enqueue(j);

        JqmEngine engine1 = new JqmEngine();
        JqmClientFactory.getClient().pauseQueuedJob(i);
        engine1.start("localhost");
        Thread.sleep(5000);

        engine1.stop();

        TypedQuery<History> query = em.createQuery("SELECT j FROM History j ORDER BY j.enqueueDate ASC", History.class);
        List<History> res1 = (ArrayList<History>) query.getResultList();
        List<JobInstance> res2 = em.createQuery("SELECT j FROM JobInstance j", JobInstance.class).getResultList();

        Assert.assertEquals(1, res1.size());
        Assert.assertEquals(1, res2.size());
        Assert.assertEquals(State.HOLDED, res2.get(0).getState());
        Assert.assertEquals(State.ENDED, res1.get(0).getState());
    }

    @Test
    public void testCancelJob() throws Exception
    {
        jqmlogger.debug("**********************************************************");
        jqmlogger.debug("Starting test testCancelJob");
        EntityManager em = Helpers.getNewEm();
        TestHelpers.cleanup(em);
        TestHelpers.createLocalNode(em);

        ArrayList<JobDefParameter> jdargs = new ArrayList<JobDefParameter>();
        JobDefParameter jdp = CreationTools.createJobDefParameter("arg", "POUPETTE", em);
        jdargs.add(jdp);

        @SuppressWarnings("unused")
        JobDef jdDemoMaven = CreationTools.createJobDef(null, true, "App", jdargs, "jqm-tests/jqm-test-datetimemaven/target/test.jar",
                TestHelpers.qVip, 42, "MarsuApplication", null, "Franquin", "ModuleMachin", "other", "other", false, em);

        JobRequest j = new JobRequest("MarsuApplication", "MAG");

        int i = JqmClientFactory.getClient().enqueue(j);
        @SuppressWarnings("unused")
        int ii = JqmClientFactory.getClient().enqueue(j);

        JqmEngine engine1 = new JqmEngine();
        JqmClientFactory.getClient().cancelJob(i);
        engine1.start("localhost");
        Thread.sleep(5000);

        engine1.stop();

        TypedQuery<History> query = em.createQuery("SELECT j FROM History j ORDER BY j.enqueueDate ASC", History.class);
        ArrayList<History> res = (ArrayList<History>) query.getResultList();

        Assert.assertEquals(2, res.size());
        Assert.assertEquals(State.CANCELLED, res.get(0).getState());
        Assert.assertEquals(State.ENDED, res.get(1).getState());
    }

    @Test
    public void testChangeQueue() throws Exception
    {
        jqmlogger.debug("**********************************************************");
        jqmlogger.debug("Starting test testChanegQueue");
        EntityManager em = Helpers.getNewEm();
        TestHelpers.cleanup(em);
        TestHelpers.createLocalNode(em);

        ArrayList<JobDefParameter> jdargs = new ArrayList<JobDefParameter>();
        JobDefParameter jdp = CreationTools.createJobDefParameter("arg", "POUPETTE", em);
        jdargs.add(jdp);

        @SuppressWarnings("unused")
        JobDef jdDemoMaven = CreationTools.createJobDef(null, true, "App", jdargs, "jqm-tests/jqm-test-datetimemaven/target/test.jar",
                TestHelpers.qVip, 42, "MarsuApplication", null, "Franquin", "ModuleMachin", "other", "other", false, em);

        JobRequest j = new JobRequest("MarsuApplication", "MAG");

        int i = JqmClientFactory.getClient().enqueue(j);

        JqmEngine engine1 = new JqmEngine();
        JqmClientFactory.getClient().setJobQueue(i, TestHelpers.qSlow.getId());

        engine1.start("localhost");
        TestHelpers.waitFor(1, 10000, em);
        engine1.stop();

        TypedQuery<History> query = em.createQuery("SELECT j FROM History j ORDER BY j.enqueueDate ASC", History.class);
        ArrayList<History> res = (ArrayList<History>) query.getResultList();

        Assert.assertEquals(1, res.size());
        Assert.assertEquals(State.ENDED, res.get(0).getState());
        Assert.assertEquals(TestHelpers.qSlow.getName(), res.get(0).getQueue().getName());
    }

    @Test
    public void testDelJobInQueue() throws Exception
    {
        jqmlogger.debug("**********************************************************");
        jqmlogger.debug("Starting test testDelJobInQueue");
        EntityManager em = Helpers.getNewEm();
        TestHelpers.cleanup(em);
        TestHelpers.createLocalNode(em);

        ArrayList<JobDefParameter> jdargs = new ArrayList<JobDefParameter>();
        JobDefParameter jdp = CreationTools.createJobDefParameter("arg", "POUPETTE", em);
        jdargs.add(jdp);

        @SuppressWarnings("unused")
        JobDef jdDemoMaven = CreationTools.createJobDef(null, true, "App", jdargs, "jqm-tests/jqm-test-datetimemaven/target/test.jar",
                TestHelpers.qVip, 42, "MarsuApplication", null, "Franquin", "ModuleMachin", "other", "other", false, em);

        JobRequest j = new JobRequest("MarsuApplication", "MAG");

        int i = JqmClientFactory.getClient().enqueue(j);

        JqmEngine engine1 = new JqmEngine();
        JqmClientFactory.getClient().deleteJob(i);
        engine1.start("localhost");
        Thread.sleep(1000);

        engine1.stop();

        TypedQuery<History> query = em.createQuery("SELECT j FROM History j ORDER BY j.enqueueDate ASC", History.class);
        ArrayList<History> res = (ArrayList<History>) query.getResultList();

        ArrayList<JobInstance> js = (ArrayList<JobInstance>) em.createQuery("SELECT j FROM JobInstance j", JobInstance.class)
                .getResultList();

        Assert.assertEquals(0, res.size());
        Assert.assertEquals(0, js.size());
    }

    @Test
    public void testResume() throws Exception
    {
        jqmlogger.debug("**********************************************************");
        jqmlogger.debug("Starting test testResume");
        EntityManager em = Helpers.getNewEm();
        TestHelpers.cleanup(em);
        TestHelpers.createLocalNode(em);

        ArrayList<JobDefParameter> jdargs = new ArrayList<JobDefParameter>();
        JobDefParameter jdp = CreationTools.createJobDefParameter("arg", "POUPETTE", em);
        jdargs.add(jdp);

        @SuppressWarnings("unused")
        JobDef jdDemoMaven = CreationTools.createJobDef(null, true, "App", jdargs, "jqm-tests/jqm-test-datetimemaven/target/test.jar",
                TestHelpers.qVip, 42, "MarsuApplication", null, "Franquin", "ModuleMachin", "other", "other", false, em);

        JobRequest j = new JobRequest("MarsuApplication", "MAG");

        int i = JqmClientFactory.getClient().enqueue(j);
        @SuppressWarnings("unused")
        int ii = JqmClientFactory.getClient().enqueue(j);

        JqmEngine engine1 = new JqmEngine();
        JqmClientFactory.getClient().pauseQueuedJob(i);
        engine1.start("localhost");
        Thread.sleep(5000);
        JqmClientFactory.getClient().resumeJob(i);
        Thread.sleep(5000);

        engine1.stop();

        TypedQuery<History> query = em.createQuery("SELECT j FROM History j ORDER BY j.enqueueDate ASC", History.class);
        ArrayList<History> res = (ArrayList<History>) query.getResultList();

        Assert.assertEquals(2, res.size());
        Assert.assertEquals(State.ENDED, res.get(0).getState());
        Assert.assertEquals(State.ENDED, res.get(1).getState());
    }

    @Test
    public void testEnqueueWithQueue() throws Exception
    {
        jqmlogger.debug("**********************************************************");
        jqmlogger.debug("Starting test testEnqueueWithQueue");
        EntityManager em = Helpers.getNewEm();
        TestHelpers.cleanup(em);
        TestHelpers.createLocalNode(em);

        CreationTools.createJobDef(null, true, "App", null, "jqm-tests/jqm-test-datetimemaven/target/test.jar", TestHelpers.qVip, 42,
                "MarsuApplication", null, "Franquin", "ModuleMachin", "other", "other", false, em);

        JobRequest j = new JobRequest("MarsuApplication", "MAG");
        j.setQueueName(TestHelpers.qNormal.getName());

        int i = JqmClientFactory.getClient().enqueue(j);

        JqmEngine engine1 = new JqmEngine();
        engine1.start("localhost");
        TestHelpers.waitFor(1, 10000, em);
        engine1.stop();

        com.enioka.jqm.api.JobInstance ji = JqmClientFactory.getClient().getJob(i);
        Assert.assertEquals(TestHelpers.qNormal.getName(), ji.getQueue().getName());
    }
}
