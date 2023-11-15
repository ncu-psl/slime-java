fn main() {
    let commentList = vec!["A"]; // Immutable binding
    let mut tweet = Tweet::new("My first tweet", SystemTime::now() - ONE_HOUR, commentList);
    commentList.push("B"); // Error: cannot borrow `commentList` as mutable, as it is not declared as mutable
}
