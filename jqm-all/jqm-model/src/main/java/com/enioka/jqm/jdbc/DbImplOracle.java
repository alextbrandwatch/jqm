package com.enioka.jqm.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DbImplOracle implements DbAdapter
{
    private final static String[] IDS = new String[] { "ID" };

    private Map<String, String> queries = new HashMap<String, String>();

    @Override
    public boolean compatibleWith(String product)
    {
        return product.contains("oracle");
    }

    @Override
    public void prepare(Connection cnx)
    {
        queries.putAll(DbImplBase.queries);
        for (Map.Entry<String, String> entry : DbImplBase.queries.entrySet())
        {
            queries.put(entry.getKey(), this.adaptSql(entry.getValue()));
        }

        queries.put("ji_update_poll",
                "UPDATE JOB_INSTANCE j1 SET NODE=?, STATUS='ATTRIBUTED', DATE_ATTRIBUTION=CURRENT_TIMESTAMP WHERE rowid IN (SELECT rid FROM (SELECT rowid as rid FROM JOB_INSTANCE j2 WHERE j2.STATUS='SUBMITTED' AND j2.QUEUE=? AND (j2.HIGHLANDER=0 OR (j2.HIGHLANDER=1 AND (SELECT COUNT(1) FROM JOB_INSTANCE j3 WHERE j3.STATUS IN('ATTRIBUTED', 'RUNNING') AND j3.JOBDEF=j2.JOBDEF)=0)) ORDER BY INTERNAL_POSITION) WHERE rownum <= ?)");
    }

    @Override
    public String adaptSql(String sql)
    {
        return sql.replace("MEMORY TABLE", "TABLE").replace(" INTEGER", " NUMBER(10, 0)").replace(" DOUBLE", " DOUBLE PRECISION")
                .replace("UNIX_MILLIS()", "JQM_PK.currval").replace("IN(UNNEST(?))", "IN(?)")
                .replace("CURRENT_TIMESTAMP - 1 MINUTE", "(SYSDATE - 1/1440)")
                .replace("CURRENT_TIMESTAMP - ? SECOND", "(SYSDATE - ?/86400)").replace("FROM (VALUES(0))", "FROM DUAL")
                .replace("BOOLEAN", "NUMBER(1)").replace("true", "1").replace("false", "0");
    }

    @Override
    public String getSqlText(String key)
    {
        return queries.get(key);
    }

    @Override
    public String[] keyRetrievalColumn()
    {
        return IDS;
    }

    @Override
    public List<String> preSchemaCreationScripts()
    {
        return new ArrayList<String>();
    }

    @Override
    public void beforeUpdate(Connection cnx, QueryPreparation q)
    {
        // It is possible to use ? parameters for arrays in Oracle, but only by using some internal Oracle objects.
        // as we do not want a dependency on this driver, even optional/provided, we do not use it.
        // We may one day revisit this using reflection.
        if (q.sqlText.contains("IN(?)"))
        {
            int index = q.sqlText.indexOf("IN(?)");
            int nbIn = 0;
            while (index >= 0)
            {
                index = q.sqlText.indexOf("IN(?)", index + 1);
                nbIn++;
            }

            int nbList = 0;
            ArrayList<Object> newParams = new ArrayList<Object>(q.parameters.size() + 10);
            for (Object o : q.parameters)
            {
                if (o instanceof List<?>)
                {
                    nbList++;
                    List<?> vv = (List<?>) o;
                    if (vv.size() == 0)
                    {
                        throw new DatabaseException("Cannot do a query whith an empty list parameter");
                    }

                    newParams.addAll(vv);

                    String newPrm[] = new String[vv.size()];
                    for (int j = 0; j < vv.size(); j++)
                    {
                        newPrm[j] = "?";
                    }
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < vv.size(); j++)
                    {
                        sb.append("?,");
                    }
                    q.sqlText = q.sqlText.replaceFirst("IN\\(\\?\\)", "IN(" + sb.substring(0, sb.length() - 1) + ")");
                }
                else
                {
                    newParams.add(o);
                }
            }
            q.parameters = newParams;

            if (nbList != nbIn)
            {
                throw new DatabaseException("Mismatch: count of list parameters and of IN clauses is different.");
            }
        }
    }

    @Override
    public void setNullParameter(int position, PreparedStatement s) throws SQLException
    {
        // Absolutely stupid: set to null regardless of type.
        s.setObject(position, null);
    }

    @Override
    public String paginateQuery(String sql, int start, int stopBefore, List<Object> prms)
    {
        int pageSize = stopBefore - start;
        sql = String.format(
                "SELECT * FROM ( SELECT /*+ FIRST_ROWS(" + pageSize + ") */ a.*, ROWNUM rnum FROM (%s) a WHERE ROWNUM < ?) WHERE RNUM >= ?",
                sql);
        // prms.add(0, pageSize);
        prms.add(stopBefore);
        prms.add(start + 1);
        return sql;
    }

}
