package y2k.joyreactor

import org.robovm.apple.uikit.*
import org.robovm.objc.annotation.CustomClass
import org.robovm.objc.annotation.IBAction
import org.robovm.objc.annotation.IBOutlet
import y2k.joyreactor.common.translate
import y2k.joyreactor.platform.StoryboardNavigation
import y2k.joyreactor.presenters.PostListPresenter

/**
 * Created by y2k on 10/11/15.
 */
@CustomClass("PostCell")
class PostCell : UITableViewCell() {

    @IBOutlet lateinit var replyCountView: UILabel
    @IBOutlet lateinit var ratingView: UILabel
    @IBOutlet lateinit var playButton: UIButton
    @IBOutlet lateinit var rateButton: UIButton

    private lateinit var presenter: PostListPresenter
    private lateinit var post: Post

    @IBAction
    internal fun rate() {
        val alert = UIAlertController()
        alert.addAction(UIAlertAction("Like".translate(), UIAlertActionStyle.Default) {
            // TODO:
        })
        alert.addAction(UIAlertAction("Dislike".translate(), UIAlertActionStyle.Destructive) {
            // TODO:
        })
        alert.addAction(UIAlertAction("Cancel".translate(), UIAlertActionStyle.Cancel, null))
        StoryboardNavigation.navigationController.presentViewController(alert, true, null)
    }

    @IBAction
    fun clicked() {
        presenter.postClicked(post)
    }

    @IBAction
    fun play() {
        presenter.playClicked(post)
    }

    fun update(presenter: PostListPresenter, post: Post) {
        this.presenter = presenter
        this.post = post

        replyCountView.text = "" + post.commentCount
        ratingView.text = "" + post.rating

        playButton.isHidden = post.image == null
        playButton.setTitle(playButtonTitle, UIControlState.Normal)
    }

    private val playButtonTitle: String
        get() = if (post.image?.isAnimated ?: false) "Play".translate() else "View".translate()
}