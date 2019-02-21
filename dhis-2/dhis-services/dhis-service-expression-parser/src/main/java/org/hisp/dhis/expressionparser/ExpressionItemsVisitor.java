package org.hisp.dhis.expressionparser;

/*
 * Copyright (c) 2004-2018, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.antlr.v4.runtime.tree.ParseTree;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hisp.dhis.common.DimensionItemType.*;
import static org.hisp.dhis.expression.ExpressionService.DAYS_DESCRIPTION;
import static org.hisp.dhis.expression.ExpressionService.SYMBOL_DAYS;
import static org.hisp.dhis.expressionparser.generated.ExpressionParser.*;

/**
 * ANTLR parse tree visitor to find the expression items in an expression,
 * find the oranisation unit groups in an expression, and also used to construct
 * an expression description by filling in the names of the items.
 * <p/>
 * Uses the ANTLR visitor partern.
 *
 * @author Jim Grace
 */
public class ExpressionItemsVisitor
    extends ExpressionVisitor
{
    @Autowired
    private DimensionService dimensionService;

    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;

    private Set<DimensionalItemObject> dimensionalItemObjects = null;

    private Set<OrganisationUnitGroup> orgUnitGroupsNeeded = null;

    public ExpressionItemsVisitor( DimensionService _dimensionService,
        OrganisationUnitGroupService _organisationUnitGroupService,
        ConstantService _constantService )
    {
        dimensionService = _dimensionService;
        organisationUnitGroupService = _organisationUnitGroupService;
        constantService = _constantService;
    }

    public Set<DimensionalItemObject> getDimensionalItemObjects( ParseTree parseTree )
    {
        dimensionalItemObjects = new HashSet<>();

        castDouble( visit( parseTree ) );

        return dimensionalItemObjects;
    }

    public Set<OrganisationUnitGroup> getOrgUnitGroups( ParseTree parseTree )
    {
        orgUnitGroupsNeeded = new HashSet<>();

        castDouble( visit( parseTree ) );

        return orgUnitGroupsNeeded;
    }

    public String getDescription( ParseTree parseTree, String expr,
        Map<String, Double> constantMap )
    {
        this.constantMap = constantMap;

        itemDescriptions = new HashMap<>();

        castDouble( visit( parseTree ) );

        String description = expr.replace( SYMBOL_DAYS, DAYS_DESCRIPTION );

        for ( Map.Entry<String, String> entry : itemDescriptions.entrySet() )
        {
            description = description.replace( entry.getKey(), entry.getValue() );
        }

        return description;
    }

    // -------------------------------------------------------------------------
    // Visitor methods implemented here
    // -------------------------------------------------------------------------

    @Override
    public Object visitDataElement ( DataElementContext ctx )
    {
        return getExpressionItem( DATA_ELEMENT, ctx.dataElementId().getText(), ctx.getText() );
    }

    @Override
    public Object visitDataElementOperandWithoutAoc ( DataElementOperandWithoutAocContext ctx )
    {
        return getExpressionItem( DATA_ELEMENT_OPERAND, ctx.dataElementOperandIdWithoutAoc().getText(), ctx.getText() );
    }

    @Override
    public Object visitDataElementOperandWithAoc ( DataElementOperandWithAocContext ctx )
    {
        return getExpressionItem( DATA_ELEMENT_OPERAND, ctx.dataElementOperandIdWithAoc().getText(), ctx.getText() );
    }

    @Override
    public Object visitProgramDataElement ( ProgramDataElementContext ctx )
    {
        return getExpressionItem( PROGRAM_DATA_ELEMENT, ctx.programDataElementId().getText(), ctx.getText() );
    }

    @Override
    public Object visitProgramAttribute ( ProgramAttributeContext ctx )
    {
        return getExpressionItem( PROGRAM_ATTRIBUTE, ctx.programAttributeId().getText(), ctx.getText() );
    }

    @Override
    public Object visitProgramIndicator ( ProgramIndicatorContext ctx )
    {
        return getExpressionItem( PROGRAM_INDICATOR, ctx.programIndicatorId().getText(), ctx.getText() );
    }

    @Override
    public final Object visitReportingRate( ReportingRateContext ctx )
    {
        return getExpressionItem( REPORTING_RATE, ctx.reportingRateId().getText(), ctx.getText() );
    };

    @Override
    public Object visitOrgUnitCount( OrgUnitCountContext ctx )
    {
        String orgUnitGroupId = ctx.orgUnitCountId().getText();

        if ( orgUnitGroupsNeeded == null && itemDescriptions == null )
        {
            return DEFAULT_ITEM_VALUE;
        }

        OrganisationUnitGroup orgUnitGroup = organisationUnitGroupService.getOrganisationUnitGroup( orgUnitGroupId );

        if ( orgUnitGroup == null )
        {
            throw new ExpressionParserExceptionWithoutContext( "Can't find organisation unit group " + orgUnitGroupId );
        }

        if ( orgUnitGroupsNeeded != null )
        {
            orgUnitGroupsNeeded.add( orgUnitGroup );
        }

        if ( itemDescriptions != null )
        {
            itemDescriptions.put( ctx.getText(), orgUnitGroup.getDisplayName() );
        }

        return DEFAULT_ITEM_VALUE;
    }

    // -------------------------------------------------------------------------
    // Logical methods implemented here
    //
    // Always visit all expressions. Need not protect against nulls because
    // dummy values will always be supplied.
    // -------------------------------------------------------------------------

    @Override
    protected final Object functionAnd( ExprContext ctx )
    {
        Boolean leftBool = castBoolean( visit( ctx.expr( 0 ) ) );
        Boolean rightBool = castBoolean( visit( ctx.expr( 1 ) ) );

        return leftBool && rightBool;
    }

    @Override
    protected final Object functionOr( ExprContext ctx )
    {
        Boolean leftBool = castBoolean( visit( ctx.expr( 0 ) ) );
        Boolean rightBool = castBoolean( visit( ctx.expr( 1 ) ) );

        return leftBool || rightBool;
    }

    @Override
    protected final Object functionIf( ExprContext ctx )
    {
        Boolean test = castBoolean( visit( ctx.a3().expr( 0 ) ) );
        Object ifTrue = visit( ctx.a3().expr( 1 ) );
        Object ifFalse = visit( ctx.a3().expr( 2 ) );

        return test ? ifTrue : ifFalse;
    }

    @Override
    protected final Object functionCoalesce( ExprContext ctx )
    {
        Object returnVal = null;

        for ( ExprContext c : ctx.a1_n().expr() )
        {
            Object val = visit( c );
            if ( returnVal == null && val != null )
            {
                returnVal = val;
            }
        }
        return returnVal;
    }

    @Override
    protected final Object functionExcept( ExprContext ctx )
    {
        // Visit the test and type-check it.
        // Always visit the expression.

        castBoolean( visit( ctx.a1().expr() ) );

        return visit( ctx.expr( 0 ) );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Object getExpressionItem( DimensionItemType type, String itemId, String exprText )
    {
        if ( dimensionalItemObjects == null && itemDescriptions == null )
        {
            return DEFAULT_ITEM_VALUE;
        }

        DimensionalItemObject item = dimensionService.getDataDimensionalItemObject( itemId );

        if ( item == null )
        {
            throw new ExpressionParserExceptionWithoutContext( "Can't find " + type.name() + " matching '" + itemId + "'" );
        }

        if ( item.getDimensionItemType() != type )
        {
            throw new ExpressionParserExceptionWithoutContext( "Expected " + type.name() + " but found " + item.getDimensionItemType().name() + " " + itemId );
        }

        if ( dimensionalItemObjects != null )
        {
            dimensionalItemObjects.add( item );
        }

        if ( itemDescriptions != null )
        {
            itemDescriptions.put( exprText, item.getDisplayName() );
        }

        return DEFAULT_ITEM_VALUE;
    }
}
