const functions = require('firebase-functions')
const admin = require('firebase-admin')
admin.initializeApp(functions.config().firebase)

exports.notifyNewUser = functions.auth.user().onCreate(event => {
	const userSnapshot = event.data
	const uid = uesrSnapshot.uid
	const userName = admin.database().ref('/users/' + uid + '/name').once('value')
	const payload = {
		notification : {
			title : '새로운 회원',
			body : userName + '회원님이 가입하셨습니다' 
		}
	}
	
	return admin.database().ref('/users').once('value').then(users => {
		const targetTokens = []
		for(var user in users.val()){
			targetTokens.push(user.val().firebaseToken)
		}		

		return admin.messaging().sendToDevice(targetTokens, payload).then(response => {
			const tokensToRemove = []
			response.results.forEach((result, index) => {
				const error = result.error

				if(error) console.log(error)
				else console.log(userName)
			})
		}) 
	})
})

exports.notifyNewComment = functions.database.ref('/comments/{postId}/{commentId}').onWrite(event => {
	const commentSnapshot = event.data
	const userUid = commentSnapshot.val().uuid
	const postId = event.params.postId
	const commentId = event.params.commentId

	return admin.database().ref('/posts/' + postId).once('value').then(post => {
		const postTitle = post.val().title

		admin.database().ref('/users/'+ userUid).once('value').then(user => {
			const userName = user.val().name
			const userProfileUrl = user.val().profileUrl

			admin.database().ref('/users').once('value').then(users => {
				const targetTokens = []
				for(var user in users.val()){
					if(userUid != user.val().firebaseToken){
						targetTokens.push(user.val().firebaseToken)		
					}
				}
				const payload = {
					notification : {
						title : '새 댓글 알림',
						body : userName + '님이 ' + postTitle + ' 게시글에 댓글을 남겼습니다',
						icon : userProfileUrl 
					}
				}

				return admin.messaging().sendToDevice(targetTokens, payload).then(result => {
					result.results.forEach((r,i) => {
						if(r.error) console.log(r.error)
						else console.log(userName)
					})
				})
			})			
		})
	})	
})

exports.notifyNewPost = functions.database.ref('/posts/{postId}').onWrite(event => {
	const postSnapshot = event.data
	const paramPostId = event.params.postId
	const userUid = postSnapshot.val().uuid
	const postTitle = postSnapshot.val().title

	return admin.database().ref('/users/' + userUid).once('value').then(user => {
		const userName = user.val().name
		const userProfileUrl = user.val().profileUrl		

		admin.database().ref('/users').once('value').then(users => {
			if(users){
				const targetTokens = []
				for(var user in users.val()){
					if(userUid != user.firebaseToken){
						targetTokens.push(user.firebaseToken)
					}		
				}
				const payload = {
					notification : {
						title : '새 게시글 알림',
						body : userName + '님이 ' + postTitle + '을(를) 게시했습니다',
						icon : userProfileUrl						
					}
				}

				return admin.messaging().sendToDevice(targetTokens, payload).then(result => {
					result.results.forEach((r,i) => {
						if(r.error) console.log(r.error)
						else console.log(userName) 
					})
				})
			}
		})		
	})		
})
