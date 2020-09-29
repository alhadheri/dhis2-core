/*
 *  Copyright (c) 2004-2020, University of Oslo
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
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package org.hisp.dhis.util;

import java.util.Date;

import org.hisp.dhis.program.notification.ProgramNotificationInstance;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateSnapshot;
import org.hisp.dhis.user.UserGroup;

import lombok.experimental.UtilityClass;

@UtilityClass
public class NotificationUtils
{

    public ProgramNotificationInstance createNotificationInstance( ProgramNotificationTemplate template,
        String date )
    {
        return createNotificationInstance( template, DateUtils.parseDate( date ) );
    }

    public ProgramNotificationInstance createNotificationInstance( ProgramNotificationTemplate template, Date date )
    {
        ProgramNotificationInstance notificationInstance = new ProgramNotificationInstance();
        notificationInstance.setAutoFields();
        notificationInstance.setName( template.getName() );
        notificationInstance.setScheduledAt( date );
        notificationInstance.setProgramNotificationTemplateSnapshot( NotificationUtils.asTemplateSnapshot( template ) );

        return notificationInstance;
    }

    private ProgramNotificationTemplateSnapshot asTemplateSnapshot( ProgramNotificationTemplate template )
    {
        return ProgramNotificationTemplateSnapshot.builder()
            .messageTemplate( template.getMessageTemplate() )
            .notificationRecipient( template.getNotificationRecipient() )
            .recipientProgramAttribute( template.getRecipientProgramAttribute() )
            .notificationTrigger( template.getNotificationTrigger() )
            .subjectTemplate( template.getSubjectTemplate() )
            .deliveryChannels( template.getDeliveryChannels() )
            .notifyParentOrganisationUnitOnly( template.getNotifyParentOrganisationUnitOnly() )
            .notifyUsersInHierarchyOnly( template.getNotifyUsersInHierarchyOnly() )
            .recipientDataElement( template.getRecipientDataElement() )
            .recipientUserGroup( buildUserGroup( template ) )
            .build();
    }

    private static UserGroup buildUserGroup( ProgramNotificationTemplate template )
    {
        /*
         * TODO: we need a deep copy of userGroup here, because we need to get rid of
         * Hibernate proxy class. moreover, for notification purposes, we don't really
         * need all informations stored in users/organizationUnits it should be enough
         * to store email, phone numbers...
         */
        return template.getRecipientUserGroup();
    }

}
