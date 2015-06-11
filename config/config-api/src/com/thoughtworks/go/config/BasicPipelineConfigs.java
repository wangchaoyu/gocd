/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.preprocessor.SkipParameterResolution;
import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.PiplineConfigVisitor;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

@ConfigTag("pipelines")
@ConfigCollection(value = PipelineConfig.class, asFieldName = "pipelines")
public class BasicPipelineConfigs extends BaseCollection<PipelineConfig> implements PipelineConfigs {

    @ConfigAttribute(value = "group", optional = true) @SkipParameterResolution
    private String group;


    @ConfigSubtag @SkipParameterResolution
    private Authorization authorization = new Authorization();

    private final ConfigErrors configErrors = new ConfigErrors();


    public BasicPipelineConfigs() {
    }

    public BasicPipelineConfigs(PipelineConfig... pipelineConfigs) {
        this(new Authorization(), pipelineConfigs);
    }

    public BasicPipelineConfigs(Authorization authorization, PipelineConfig... pipelineConfigs) {
        super(pipelineConfigs);
        this.authorization = authorization;
    }

    public BasicPipelineConfigs(String group, Authorization authorization, PipelineConfig... pipelineConfigs) {
        super(pipelineConfigs);
        this.group = group;
        this.authorization = authorization;
    }

    @Override
    public PipelineConfig findBy(final CaseInsensitiveString pipelineName) {
        for (int i=0; i< this.size(); i++) {
            PipelineConfig pipelineConfig = this.get(i);
            if (pipelineConfig.name().equals(pipelineName)) {
                return pipelineConfig;
            }
        }
        return null;
    }

    @Override
    public boolean add(PipelineConfig pipelineConfig) {
        verifyUniqueName(pipelineConfig);
        return addWithoutValidation(pipelineConfig);
    }

    @Override
    public boolean addWithoutValidation(PipelineConfig pipelineConfig) {
        return super.add(pipelineConfig);
    }

    @Override
    public PipelineConfig set(int index, PipelineConfig pipelineConfig) {
        verifyUniqueName(pipelineConfig, index);
        return super.set(index, pipelineConfig);
    }

    @Override
    public void addToTop(PipelineConfig pipelineConfig) {
        this.add(0, pipelineConfig);
    }

    @Override
    public void add(int index, PipelineConfig pipelineConfig) {
        verifyUniqueName(pipelineConfig);
        super.add(index, pipelineConfig);
    }

    private void verifyUniqueName(PipelineConfig pipelineConfig) {
        if (alreadyContains(pipelineConfig)) {
            throw bomb("You have defined multiple pipelines called '" + pipelineConfig.name() + "'. Pipeline names must be unique.");
        }
    }

    private void verifyUniqueName(PipelineConfig pipelineConfig, int index) {
        if (pipelineConfig.name().equals(super.get(index).name())) {
            return;
        }
        verifyUniqueName(pipelineConfig);
    }


    private boolean alreadyContains(PipelineConfig pipelineConfig) {
        return findBy(pipelineConfig.name()) != null;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public void setGroup(String group) {
        this.group = sanitizedGroupName(group);
    }

    public static String sanitizedGroupName(String group) {
        return StringUtils.isBlank(group) ? DEFAULT_GROUP : group;
    }

    @Override
    public boolean isNamed(String groupName) {
        return group.equals(groupName);
    }


    @Override
    public void update(String groupName, PipelineConfig pipeline, String pipelineName) {
        if (!isSameGroup(groupName)) {
            return;
        }
        this.set(getIndex(pipelineName), pipeline);
    }

    private int getIndex(String pipelineName) {
        return this.indexOf(this.findBy(new CaseInsensitiveString(pipelineName)));
    }

    @Override
    public boolean save(PipelineConfig pipeline, String groupName) {
        if (isSameGroup(groupName)) {
            this.addToTop(pipeline);
            return true;
        } else {
            return false;
        }
    }

    private boolean isSameGroup(String groupName) {
        return StringUtils.equals(groupName, this.getGroup());
    }

    @Override
    public void add(List<String> allGroup) {
        allGroup.add(group);
    }

    @Override
    public boolean exist(int pipelineIndex) {
        return pipelineIndex < this.size();
    }

    @Override
    public boolean hasPipeline(CaseInsensitiveString pipelineName) {
        for (PipelineConfig pipelineConfig : this) {
            if (pipelineConfig.name().equals(pipelineName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Authorization getAuthorization() {
        return this.authorization;
    }

    @Override
    public void accept(PiplineConfigVisitor visitor) {
        for (PipelineConfig pipelineConfig : this) {
            visitor.visit(pipelineConfig);
        }
    }

    @Override
    public void setAuthorization(Authorization authorization) {
        this.authorization = authorization;
    }

    @Override
    public boolean hasViewPermission(final CaseInsensitiveString username, UserRoleMatcher userRoleMatcher) {
        return !hasAuthorizationDefined() || authorization.hasViewPermission(username, userRoleMatcher);
    }

    @Override
    public boolean hasViewPermissionDefined() {
        return authorization.hasViewPermissionDefined();
    }

    @Override
    public boolean hasOperationPermissionDefined() {
        return authorization.hasOperationPermissionDefined();
    }

    @Override
    public boolean hasOperatePermission(final CaseInsensitiveString username, UserRoleMatcher userRoleMatcher) {
        return !hasAuthorizationDefined() || authorization.hasOperatePermission(username, userRoleMatcher);
    }

    @Override
    public boolean hasAuthorizationDefined() {
        return !authorization.equals(new Authorization());
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        BasicPipelineConfigs pipelines = (BasicPipelineConfigs) o;

        if (authorization != null ? !authorization.equals(pipelines.authorization) : pipelines.authorization != null) {
            return false;
        }
        if (group != null ? !group.equals(pipelines.group) : pipelines.group != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (group != null ? group.hashCode() : 0);
        result = 31 * result + (authorization != null ? authorization.hashCode() : 0);
        return result;
    }

    @Override
    public boolean hasTemplate() {
        for (PipelineConfig pipelineConfig : this) {
            if (pipelineConfig.hasTemplate()) { return true; }
        }
        return false;
    }

    @Override
    public PipelineConfigs getCopyForEditing() {
        PipelineConfigs clone = (PipelineConfigs) clone();
        clone.clear();
        for (PipelineConfig pipeline : this) {
            clone.add(pipeline.getCopyForEditing());
        }
        return clone;
    }

    @Override
    public boolean isUserAnAdmin(final CaseInsensitiveString userName, List<Role> memberRoles) {
        return authorization.hasAdminsDefined() && authorization.isUserAnAdmin(userName, memberRoles);
    }

    @Override
    public void validate(ValidationContext validationContext) {
        if (StringUtils.isBlank(group) || !new NameTypeValidator().isNameValid(group)) {
            this.configErrors.add(GROUP, NameTypeValidator.errorMessage("group", group));
        }

        verifyPipelineNameUniqueness();
    }

    private void verifyPipelineNameUniqueness() {
        HashMap<String, PipelineConfig> hashMap = new HashMap<String, PipelineConfig>();
        for(PipelineConfig pipelineConfig : this){
            pipelineConfig.validateNameUniqueness(hashMap);
        }
    }

    @Override
    public void validateNameUniqueness(Map<String, PipelineConfigs> groupNameMap) {
        String currentName = group.toLowerCase();
        PipelineConfigs groupWithSameName = groupNameMap.get(currentName);
        if (groupWithSameName == null) {
            groupNameMap.put(currentName, this);
        } else {
            groupWithSameName.addError(GROUP, createNameConflictError());
            this.nameConflictError();
        }
    }

    private void nameConflictError() {
        this.configErrors.add(GROUP, createNameConflictError());
    }

    private String createNameConflictError() {
        return String.format("Group with name '%s' already exists", group);
    }

    @Override
    public ConfigErrors errors() {
        return configErrors;
    }

	@Override
    public List<PipelineConfig> getPipelines() {
		return this;
	}

    @Override
    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    @Override
    public List<AdminUser> getOperateUsers() {
        return authorization.getOperationConfig().getUsers();
    }

    @Override
    public List<AdminRole> getOperateRoles() {
        return authorization.getOperationConfig().getRoles();
    }

    @Override
    public List<String> getOperateRoleNames() {
        List<String> roles = new ArrayList<String>();
        for (AdminRole role : getOperateRoles()) {
            roles.add(CaseInsensitiveString.str(role.getName()));
        }
        return roles;
    }

    @Override
    public List<String> getOperateUserNames() {
        List<String> users = new ArrayList<String>();
        for (AdminUser user : getOperateUsers()) {
            users.add(CaseInsensitiveString.str(user.getName()));
        }
        return users;
    }

    @Override
    public void setConfigAttributes(Object attributes) {
        Map attributeMap = (Map) attributes;
        if (attributeMap == null) {
            return;
        }
        if (attributeMap.containsKey(GROUP)) {
            this.group = (String) attributeMap.get(GROUP);
        }
        if (attributeMap.containsKey(AUTHORIZATION)) {
            this.authorization = new Authorization();
            this.authorization.setConfigAttributes(attributeMap.get(AUTHORIZATION));
        }
        else {
            this.authorization = new Authorization();
        }
    }


    @Override
    public void cleanupAllUsagesOfRole(Role roleToDelete) {
        getAuthorization().removeAllUsagesOfRole(roleToDelete);
        for (PipelineConfig pipelineConfig : this){
            pipelineConfig.cleanupAllUsagesOfRole(roleToDelete);
        }
    }
}
