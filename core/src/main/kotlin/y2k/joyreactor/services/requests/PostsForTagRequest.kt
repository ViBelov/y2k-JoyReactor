package y2k.joyreactor.services.requests

import org.jsoup.nodes.Element
import rx.Observable
import rx.schedulers.Schedulers
import y2k.joyreactor.Image
import y2k.joyreactor.Post
import y2k.joyreactor.Tag
import y2k.joyreactor.http.HttpClient
import java.util.*
import java.util.regex.Pattern

/**
 * Created by y2k on 9/26/15.
 */
class PostsForTagRequest {

    fun requestAsync(tagId: Tag, pageId: String? = null): Observable<Data> {
        return Observable
            .fromCallable {
                val url = UrlBuilder().build(tagId, pageId)
                val doc = HttpClient.instance.getDocument(url)

                val posts = ArrayList<Post>()
                for (e in doc.select("div.postContainer"))
                    posts.add(newPost(e))

                val next = doc.select("a.next").first()
                Data(posts, next?.let { extractNumberFromEnd(next.attr("href")) })
            }
            .subscribeOn(Schedulers.io())
    }

    internal class PostParser(private val element: Element) {

        val commentCount: Int
            get() {
                val e = element.select("a.commentnum").first()
                val m = COMMENT_COUNT_REGEX.matcher(e.text())
                if (!m.find()) throw IllegalStateException()
                return Integer.parseInt(m.group())
            }

        val rating: Float
            get() {
                val e = element.select("span.post_rating > span").first()
                val m = RATING_REGEX.matcher(e.text())
                return if (m.find()) java.lang.Float.parseFloat(m.group()) else 0f
            }

        val created: Date
            get() {
                val e = element.select("span.date > span")
                return Date(1000L * java.lang.Long.parseLong(e.attr("data-time")))
            }

        companion object {

            private val COMMENT_COUNT_REGEX = Pattern.compile("\\d+")
            private val RATING_REGEX = Pattern.compile("[\\d\\.]+")
        }
    }

    internal class ThumbnailParser(private val element: Element) {

        fun load(): Image? {
            val img = element.select("div.post_content img").first()
            if (img != null && img.hasAttr("width")) {
                return Image(
                    if (hasFull(img))
                        img.parent().attr("href").replace("(/full/).+(-\\d+\\.)".toRegex(), "$1$2")
                    else
                        img.attr("src").replace("(/post/).+(-\\d+\\.)".toRegex(), "$1$2"),
                    Integer.parseInt(img.attr("width")),
                    Integer.parseInt(img.attr("height")))
            }
            return null
        }

        private fun hasFull(img: Element): Boolean {
            return "a" == img.parent().tagName()
        }
    }

    internal class YoutubeThumbnailParser(private val element: Element) {

        fun load(): Image? {
            val iframe = element.select("iframe.youtube-player").first() ?: return null
            val m = SRC_PATTERN.matcher(iframe.attr("src"))
            if (!m.find()) throw IllegalStateException(iframe.attr("src"))
            return Image(
                "http://img.youtube.com/vi/" + m.group(1) + "/0.jpg",
                Integer.parseInt(iframe.attr("width")),
                Integer.parseInt(iframe.attr("height")))
        }

        companion object {

            private val SRC_PATTERN = Pattern.compile("/embed/([^\\?]+)")
        }
    }

    internal class VideoThumbnailParser(private val element: Element) {

        fun load(): Image? {
            val video = element.select("video[poster]").first() ?: return null
            try {
                return Image(
                    element.select("span.video_gif_holder > a").first().attr("href").replace("(/post/).+(-)".toRegex(), "$1$2"),
                    Integer.parseInt(video.attr("width")),
                    Integer.parseInt(video.attr("height")))
            } catch (e: Exception) {
                println("ELEMENT | " + video)
                throw e
            }
        }
    }

    class Data(val posts: List<Post>, val nextPage: String?)

    companion object {

        internal fun newPost(element: Element): Post {
            var image = ThumbnailParser(element).load()
            if (image == null) image = YoutubeThumbnailParser(element).load()
            if (image == null) image = VideoThumbnailParser(element).load()

            val parser = PostParser(element)

            return Post(
                element.select("div.post_content").text(),
                image,
                element.select("div.uhead_nick > img").attr("src"),
                element.select("div.uhead_nick > a").text(),
                parser.created,
                extractNumberFromEnd(element.id()),
                parser.commentCount,
                parser.rating)
        }

        private fun extractNumberFromEnd(text: String): String {
            val m = Pattern.compile("\\d+$").matcher(text)
            if (!m.find()) throw IllegalStateException()
            return m.group()
        }
    }
}