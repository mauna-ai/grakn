/*
 * GRAKN.AI - THE KNOWLEDGE GRAPH
 * Copyright (C) 2018 Grakn Labs Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package grakn.core.graql.reasoner.reasoning;

import grakn.core.graql.answer.ConceptMap;
import grakn.core.graql.query.GetQuery;
import grakn.core.graql.query.Graql;
import grakn.core.rule.GraknTestServer;
import grakn.core.server.Session;
import grakn.core.server.Transaction;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static grakn.core.util.GraqlTestUtil.loadFromFileAndCommit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("Duplicates")
public class TypeGenerationIT {
    private static String resourcePath = "test-integration/graql/reasoner/stubs/";

    @ClassRule
    public static final GraknTestServer server = new GraknTestServer();

    @Test //Expected result: The query should return a unique match.
    public void generatingMultipleIsaEdges() {
        try(Session session = server.sessionWithNewKeyspace()) {
            loadFromFileAndCommit(resourcePath, "typeDerivation.gql", session);
            try (Transaction tx = session.transaction(Transaction.Type.WRITE)) {
                String derivedTypeQuery = "match $x isa derivedEntity; get;";
                List<ConceptMap> answers = tx.execute(Graql.<GetQuery>parse(derivedTypeQuery));
                assertEquals(1, answers.size());

                String variableTypeQuery = "match $x isa $type; get;";
                List<ConceptMap> answers2 = tx.execute(Graql.<GetQuery>parse(variableTypeQuery));
                assertEquals(4, answers2.size());
                answers2.forEach(ans -> assertEquals(2, ans.size()));
            }
        }
    }

    @Test //Expected result: Differentiated behaviour based on directedness of the isa.
    public void generatingIsaEdgesDirectly() {
        try(Session session = server.sessionWithNewKeyspace()) {
            loadFromFileAndCommit(resourcePath, "typeDerivationWithDirect.gql", session);
            try (Transaction tx = session.transaction(Transaction.Type.WRITE)) {
                String queryString = "match $x isa derivedEntity; get;";
                String queryString2 = "match $x isa! derivedEntity; get;";
                String queryString3 = "match $x isa directDerivedEntity; get;";
                List<ConceptMap> answers = tx.execute(Graql.<GetQuery>parse(queryString));
                List<ConceptMap> answers2 = tx.execute(Graql.<GetQuery>parse(queryString2));
                List<ConceptMap> answers3 = tx.execute(Graql.<GetQuery>parse(queryString3));
                assertEquals(2, answers.size());
                assertEquals(2, answers2.size());
                assertEquals(1, answers3.size());
            }
        }
    }

    @Test //Expected result: Differentiated behaviour based on directedness of the isa.
    public void generatingIsaEdgesForRelationsDirectly() {
        try(Session session = server.sessionWithNewKeyspace()) {
            loadFromFileAndCommit(resourcePath, "typeDerivationRelationsWithDirect.gql", session);
            try (Transaction tx = session.transaction(Transaction.Type.WRITE)) {
                String queryString = "match ($x, $y) isa derivedRelation; get;";
                String queryString2 = "match ($x, $y) isa! derivedRelation; get;";
                String queryString3 = "match ($x, $y) isa directDerivedRelation; get;";
                List<ConceptMap> answers = tx.execute(Graql.<GetQuery>parse(queryString));
                List<ConceptMap> answers2 = tx.execute(Graql.<GetQuery>parse(queryString2));
                List<ConceptMap> answers3 = tx.execute(Graql.<GetQuery>parse(queryString3));
                assertEquals(2, answers.size());
                assertEquals(2, answers2.size());
                assertEquals(1, answers3.size());
            }
        }
    }

    @Test //Expected result: The queries should return the same two matches.
    public void generatingIsaEdgeFromRelations() {
        try(Session session = server.sessionWithNewKeyspace()) {
            loadFromFileAndCommit(resourcePath, "typeDerivationFromRelations.gql", session);
            try (Transaction tx = session.transaction(Transaction.Type.WRITE)) {
                String queryString = "match $x isa baseEntity; get;";
                String queryString2 = "match $x isa derivedEntity; get;";
                List<ConceptMap> answers = tx.execute(Graql.<GetQuery>parse(queryString));
                List<ConceptMap> answers2 = tx.execute(Graql.<GetQuery>parse(queryString2));
                assertEquals(2, answers.size());
                assertTrue(answers.containsAll(answers2));
                assertTrue(answers2.containsAll(answers));
            }
        }
    }

    @Test //Expected result: The queries should return the same two matches.
    public void generatingIsaEdgeFromAttribute() {
        try(Session session = server.sessionWithNewKeyspace()) {
            loadFromFileAndCommit(resourcePath, "typeDerivationFromAttribute.gql", session);
            try (Transaction tx = session.transaction(Transaction.Type.WRITE)) {
                String queryString = "match $x isa baseEntity; get;";
                String queryString2 = "match $x isa derivedEntity; get;";
                List<ConceptMap> answers = tx.execute(Graql.<GetQuery>parse(queryString));
                List<ConceptMap> answers2 = tx.execute(Graql.<GetQuery>parse(queryString2));
                assertEquals(tx.getAttributeType("baseAttribute").instances().count(), answers.size());
                assertTrue(answers.containsAll(answers2));
                assertTrue(answers2.containsAll(answers));
            }
        }
    }

    //TODO: currently disallowed by rule validation
    @Ignore
    @Test //Expected result: The queries should return different matches, unique per query.
    public void generatingFreshEntity() {
        try(Session session = server.sessionWithNewKeyspace()) {
            loadFromFileAndCommit(resourcePath, "freshEntityDerivation.gql", session);
            try (Transaction tx = session.transaction(Transaction.Type.WRITE)) {
                String queryString = "match $x isa baseEntity; get;";
                String queryString2 = "match $x isa derivedEntity; get;";
                List<ConceptMap> answers = tx.execute(Graql.<GetQuery>parse(queryString));
                List<ConceptMap> answers2 = tx.execute(Graql.<GetQuery>parse(queryString2));
                assertEquals(answers.size(), answers2.size());
                assertFalse(answers.containsAll(answers2));
                assertFalse(answers2.containsAll(answers));
            }
        }
    }

    //TODO: currently disallowed by rule validation
    @Ignore
    @Test
    //Expected result: The query should return a unique match (or possibly nothing if we enforce range-restriction).
    public void generatingFreshEntity2() {
        try(Session session = server.sessionWithNewKeyspace()) {
            loadFromFileAndCommit(resourcePath, "freshEntityDerivationFromRelations.gql", session);
            try (Transaction tx = session.transaction(Transaction.Type.WRITE)) {
                String queryString = "match $x isa derivedEntity; get;";
                String explicitQuery = "match $x isa baseEntity; get;";
                List<ConceptMap> answers = tx.execute(Graql.<GetQuery>parse(queryString));
                List<ConceptMap> answers2 = tx.execute(Graql.<GetQuery>parse(explicitQuery), false);

                assertEquals(3, answers2.size());
                assertTrue(!answers2.containsAll(answers));
            }
        }
    }

    //TODO: currently disallowed by rule validation
    @Ignore
    @Test //Expected result: The query should return three different instances of relation1 with unique ids.
    public void generatingFreshRelation() {
        try(Session session = server.sessionWithNewKeyspace()) {
            loadFromFileAndCommit(resourcePath, "freshRelationDerivation.gql", session);
            try (Transaction tx = session.transaction(Transaction.Type.WRITE)) {
                String queryString = "match $x isa baseRelation; get;";
                List<ConceptMap> answers = tx.execute(Graql.<GetQuery>parse(queryString));
                assertEquals(3, answers.size());
            }
        }
    }
}
