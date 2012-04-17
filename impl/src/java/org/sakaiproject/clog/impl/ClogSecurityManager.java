/*************************************************************************************
 * Copyright 2006, 2008 Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 *       http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.

 *************************************************************************************/

package org.sakaiproject.clog.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.sakaiproject.clog.api.datamodel.Post;
import org.sakaiproject.clog.api.ClogFunctions;
import org.sakaiproject.clog.api.SakaiProxy;

public class ClogSecurityManager {
    private Logger logger = Logger.getLogger(ClogSecurityManager.class);

    private SakaiProxy sakaiProxy;

    public ClogSecurityManager(SakaiProxy sakaiProxy) {
	this.sakaiProxy = sakaiProxy;
    }

    public boolean canCurrentUserCommentOnPost(Post post) {
	if (logger.isDebugEnabled())
	    logger.debug("canCurrentUserCommentOnPost()");

	// if(sakaiProxy.isOnGateway() && post.isPublic() &&
	// post.isCommentable())
	// return true;

	// If the post is comment-able and the current user has
	// blog.comment.create
	if (post.isCommentable() && sakaiProxy.isAllowedFunction(ClogFunctions.CLOG_COMMENT_CREATE, post.getSiteId())) {
	    return true;
	}

	// An author can always comment on their own posts
	if (post.getCreatorId().equals(sakaiProxy.getCurrentUserId())) {
	    return true;
	}

	return false;
    }

    public boolean canCurrentUserDeletePost(Post post) throws SecurityException {
	if (sakaiProxy.isAllowedFunction(ClogFunctions.CLOG_POST_DELETE_ANY, post.getSiteId())) {
	    return true;
	}

	String currentUser = sakaiProxy.getCurrentUserId();

	// If the current user is the author and has blog.post.delete.own
	if (currentUser != null && currentUser.equals(post.getCreatorId()) && sakaiProxy.isAllowedFunction(ClogFunctions.CLOG_POST_DELETE_OWN, post.getSiteId())) {
	    return true;
	}

	return false;
    }

    public boolean canCurrentUserEditPost(Post post) {
	// This acts as an override
	if (sakaiProxy.isAllowedFunction(ClogFunctions.CLOG_POST_UPDATE_ANY, post.getSiteId())) {
	    return true;
	}

	// If it's public and not marked read only, yes.
	if (post.isPublic()) {
	    return true;
	}

	String currentUser = sakaiProxy.getCurrentUserId();

	// If the current user is authenticated and the post author, yes.
	if (currentUser != null && currentUser.equals(post.getCreatorId()) && sakaiProxy.isAllowedFunction(ClogFunctions.CLOG_POST_UPDATE_OWN, post.getSiteId())) {
	    return true;
	}

	// If the user is authenticated and the post is not marked read only,
	// yes
	if (currentUser != null) {
	    return true;
	}

	return false;
    }

    /**
     * Tests whether the current user can read each Post and if not, filters
     * that post out of the resulting list
     */
    public List<Post> filter(List<Post> posts) {
	List<Post> filtered = new ArrayList<Post>();
	for (Post post : posts) {
	    if (canCurrentUserReadPost(post)) {
		filtered.add(post);
	    }
	}

	return filtered;
    }

    public boolean canCurrentUserReadPost(Post post) {
	
	boolean maintainer = sakaiProxy.isCurrentUserMaintainer(post.getSiteId());
	
	// If the post is public, yes.
	if (post.isPublic()) {
	    return true;
	}
	
	try {
	    if(post.isVisibleToSite() && sakaiProxy.isAllowedFunction(ClogFunctions.CLOG_POST_READ_ANY, post.getSiteId())) {
		return true;
	    }
	}
	catch(Exception e) {
	    logger.error("Exception during security check.",e);
	    return false;
	}
	
	try {
	    if(post.isVisibleToMaintainers() && maintainer) {
		return true;
	    }
	}
	catch(Exception e) {
	    logger.error("Exception during security check.",e);
	    return false;
	}

	// Only maintainers can view recycled posts
	if (post.isRecycled() && maintainer) {
	    return true;
	}

	// Allow search to index posts
	String threadName = Thread.currentThread().getName();
	if (!post.isPrivate() && "IndexManager".equals(threadName)) {
	    return true;
	}

	String currentUser = sakaiProxy.getCurrentUserId();

	// If the current user is authenticated and the post author, yes.
	if (currentUser != null && currentUser.equals(post.getCreatorId())) {
	    return true;
	}

	return false;
    }
}
