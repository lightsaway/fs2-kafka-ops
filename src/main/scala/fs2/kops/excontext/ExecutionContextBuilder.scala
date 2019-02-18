package fs2.kops.excontext

trait ContextBuilder {
  def fixedThreadPool[F[_]] = new FixedThreadPool[F]
  def cachedThreadPool[F[_]] = new CachedhreadPool[F]
}
