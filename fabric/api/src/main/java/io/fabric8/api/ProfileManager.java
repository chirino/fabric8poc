/*
 * #%L
 * Fabric8 :: API
 * %%
 * Copyright (C) 2014 Red Hat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.fabric8.api;

import java.util.Set;

import org.jboss.gravia.resource.Version;
import org.jboss.gravia.runtime.Runtime;

/**
 * A profile manager
 *
 * An instance of this service can be obtained from the gravia {@link Runtime}.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface ProfileManager {

    /**
     * Aquire an exclusive write lock for the given profile version
     */
    LockHandle aquireProfileVersionLock(Version version);

    /**
     * Get the default profile version
     */
    ProfileVersion getDefaultProfileVersion();

    /**
     * Get the default profile
     */
    Profile getDefaultProfile();

    /**
     * Get the set of profile version identities in the cluster
     */
    Set<Version> getVersions();

    /**
     * Get the set of profile versions for the given identities
     * @param identities The requested identities or <code>null</code> for all profile versions
     */
    Set<ProfileVersion> getProfileVersions(Set<Version> identities);

    /**
     * Get the profile version for the given identity
     */
    ProfileVersion getProfileVersion(Version identity);

    /**
     * Get the linked profile version for the given identity
     */
    LinkedProfileVersion getLinkedProfileVersion(Version identity);

    /**
     * Add a profile version
     */
    ProfileVersion addProfileVersion(ProfileVersion profileVersion);

    /**
     * Remove a profile version
     */
    ProfileVersion removeProfileVersion(Version identity);

    /**
     * Get the profile idetities for a given version
     */
    Set<String> getProfileIdentities(Version version);

    /**
     * Get the profiles for a given version and identities
     * @param identities The requested identities or <code>null</code> for all profiles
     */
    Set<Profile> getProfiles(Version version, Set<String> identities);

    /**
     * Get the profile for a given identity and version
     */
    Profile getProfile(Version version, String identity);

    /**
     * Get the effective profile for a given identity and version
     */
    Profile getEffectiveProfile(Version version, String identity);

    /**
     * Get the linked profile for the given identity
     */
    LinkedProfile getLinkedProfile(Version version, String identity);

    /**
     * Add a profile to the given version
     */
    Profile addProfile(Version version, Profile profile);

    /**
     * Remove a profile from the given version
     */
    Profile removeProfile(Version version, String identity);

    /**
     * Update the given profile
     */
    Profile updateProfile(Profile profile, ProfileEventListener listener);
}
